from dataclasses import dataclass, field


@dataclass
class Author:
    position: int
    keyname: str
    forenames: str = ""


@dataclass
class ArxivRecord:
    arxiv_id: str
    oai_identifier: str
    oai_datestamp: str
    is_deleted: bool
    title: str = ""
    abstract: str = ""
    categories: str = ""
    primary_category: str = ""
    license: str = ""
    submitter: str = ""
    created_date: str = ""
    updated_date: str = ""
    authors: list = field(default_factory=list)


@dataclass
class ParsedResponse:
    records: list
    resumption_token: str | None
    is_final_page: bool


@dataclass
class HarvestState:
    last_harvest_date: str | None
    resumption_token: str | None
    total_harvested: int
