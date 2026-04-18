import logging
from datetime import date

from .client import OaiPmhClient, HarvesterError
from .database import Database
from .parser import OaiPmhParser, OaiPmhError

log = logging.getLogger(__name__)


class Harvester:
    def __init__(self, client: OaiPmhClient, parser: OaiPmhParser, db: Database, set_spec: str, metadata_prefix: str):
        self.client = client
        self.parser = parser
        self.db = db
        self.set_spec = set_spec
        self.metadata_prefix = metadata_prefix

    def run(self, from_date: str | None = None, until_date: str | None = None, resume: bool = True, max_records: int | None = None):
        state = self.db.load_harvest_state()
        token = None
        total = 0

        if resume and state and state.resumption_token:
            token = state.resumption_token
            total = state.total_harvested
            log.info("Resuming from saved token (already harvested: %d)", total)
        else:
            if from_date is None and state and state.last_harvest_date:
                from_date = state.last_harvest_date
                log.info("Incremental harvest from %s", from_date)
            elif from_date:
                log.info("Harvesting from %s", from_date)
            else:
                log.info("Full harvest (no start date)")

        first_request = token is None

        while True:
            try:
                if token:
                    xml_text = self.client.list_records_resumption(token)
                else:
                    xml_text = self.client.list_records(self.set_spec, self.metadata_prefix, from_date, until_date)
            except HarvesterError as e:
                log.error("Fatal HTTP error: %s", e)
                raise

            try:
                parsed = self.parser.parse_response(xml_text)
            except OaiPmhError as e:
                if e.code == "badResumptionToken":
                    log.warning("resumptionToken expired — restarting from from_date=%s", from_date)
                    token = None
                    first_request = True
                    continue
                if e.code == "noRecordsMatch":
                    log.info("No records match the query — harvest complete")
                    break
                log.error("OAI-PMH error: %s", e)
                raise

            records = parsed.records
            if max_records is not None:
                remaining = max_records - total
                records = records[:remaining]

            batch_size = len(records)
            self.db.upsert_papers_batch(records)
            total += batch_size
            token = parsed.resumption_token

            self.db.save_harvest_state(from_date, token, total)
            log.info("Batch: %d records saved (total so far: %d)", batch_size, total)

            if (max_records is not None and total >= max_records) or parsed.is_final_page:
                final_date = until_date or date.today().isoformat()
                self.db.save_harvest_state(final_date, None, total)
                log.info("Harvest complete. Total records: %d, next from_date: %s", total, final_date)
                break

            first_request = False
