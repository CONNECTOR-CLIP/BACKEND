import logging
import re
import xml.etree.ElementTree as ET

from .models import ArxivRecord, Author, ParsedResponse

log = logging.getLogger(__name__)

NS_OAI = "http://www.openarchives.org/OAI/2.0/"
NS_ARXIV = "http://arxiv.org/OAI/arXiv/"

_RE_WHITESPACE = re.compile(r"\s+")


def _tag(ns: str, name: str) -> str:
    return f"{{{ns}}}{name}"


def _text(el, ns: str, name: str, default: str = "") -> str:
    child = el.find(_tag(ns, name))
    return child.text.strip() if child is not None and child.text else default


def _normalize(text: str) -> str:
    return _RE_WHITESPACE.sub(" ", text).strip()


class OaiPmhError(Exception):
    def __init__(self, code: str, message: str):
        super().__init__(f"OAI-PMH error [{code}]: {message}")
        self.code = code


class OaiPmhParser:
    def parse_response(self, xml_text: str) -> ParsedResponse:
        try:
            root = ET.fromstring(xml_text)
        except ET.ParseError as e:
            log.error("Malformed XML (first 500 chars): %s ... — %s", xml_text[:500], e)
            return ParsedResponse(records=[], resumption_token=None, is_final_page=True)

        # Check for OAI-PMH errors
        error_el = root.find(_tag(NS_OAI, "error"))
        if error_el is not None:
            code = error_el.get("code", "unknown")
            message = (error_el.text or "").strip()
            raise OaiPmhError(code, message)

        list_records_el = root.find(_tag(NS_OAI, "ListRecords"))
        if list_records_el is None:
            log.warning("No <ListRecords> element in response")
            return ParsedResponse(records=[], resumption_token=None, is_final_page=True)

        records = []
        for record_el in list_records_el.findall(_tag(NS_OAI, "record")):
            record = self._parse_record(record_el)
            if record is not None:
                records.append(record)

        resumption_token, is_final_page = self._extract_resumption_token(list_records_el)
        return ParsedResponse(records=records, resumption_token=resumption_token, is_final_page=is_final_page)

    def _parse_record(self, record_el) -> ArxivRecord | None:
        header_el = record_el.find(_tag(NS_OAI, "header"))
        if header_el is None:
            return None

        identifier = _text(header_el, NS_OAI, "identifier")
        datestamp = _text(header_el, NS_OAI, "datestamp")
        is_deleted = header_el.get("status") == "deleted"

        arxiv_id = identifier.replace("oai:arXiv.org:", "").strip()

        record = ArxivRecord(
            arxiv_id=arxiv_id,
            oai_identifier=identifier,
            oai_datestamp=datestamp,
            is_deleted=is_deleted,
        )

        if is_deleted:
            return record

        metadata_el = record_el.find(_tag(NS_OAI, "metadata"))
        if metadata_el is None:
            return record

        arxiv_el = metadata_el.find(_tag(NS_ARXIV, "arXiv"))
        if arxiv_el is None:
            return record

        record.title = _normalize(_text(arxiv_el, NS_ARXIV, "title"))
        record.abstract = _normalize(_text(arxiv_el, NS_ARXIV, "abstract"))
        record.categories = _text(arxiv_el, NS_ARXIV, "categories")
        record.license = _text(arxiv_el, NS_ARXIV, "license")
        record.submitter = _text(arxiv_el, NS_ARXIV, "submitter")
        record.created_date = _text(arxiv_el, NS_ARXIV, "created")
        record.updated_date = _text(arxiv_el, NS_ARXIV, "updated")

        cats = record.categories.split()
        record.primary_category = cats[0] if cats else ""

        authors_el = arxiv_el.find(_tag(NS_ARXIV, "authors"))
        if authors_el is not None:
            record.authors = self._parse_authors(authors_el)

        return record

    def _parse_authors(self, authors_el) -> list:
        authors = []
        for pos, author_el in enumerate(authors_el.findall(_tag(NS_ARXIV, "author"))):
            keyname = _text(author_el, NS_ARXIV, "keyname")
            forenames = _text(author_el, NS_ARXIV, "forenames")
            authors.append(Author(position=pos, keyname=keyname, forenames=forenames))
        return authors

    def _extract_resumption_token(self, list_records_el) -> tuple:
        token_el = list_records_el.find(_tag(NS_OAI, "resumptionToken"))
        if token_el is None:
            return None, True
        token_text = (token_el.text or "").strip()
        if not token_text:
            return None, True
        return token_text, False
