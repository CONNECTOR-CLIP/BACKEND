import argparse
import logging
import sys
from datetime import date

from harvester.client import OaiPmhClient
from harvester.database import Database
from harvester.harvester import Harvester
from harvester.parser import OaiPmhParser

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
OAI_ENDPOINT = "https://oaipmh.arxiv.org/oai"
OAI_SET = "cs"
METADATA_PREFIX = "arXiv"
RATE_LIMIT_SECS = 5.0
REQUEST_TIMEOUT = 30
MAX_RETRIES = 5
RETRY_BACKOFF = [5, 15, 30, 60, 120]
DB_DSN = "postgresql://localhost/arxiv_cs"
MAX_RECORDS = 100
LOG_FILE = "arxiv_harvester.log"
# ---------------------------------------------------------------------------


def setup_logging(log_file: str):
    fmt = "%(asctime)s %(levelname)s %(name)s — %(message)s"
    logging.basicConfig(
        level=logging.INFO,
        format=fmt,
        handlers=[
            logging.FileHandler(log_file, encoding="utf-8"),
            logging.StreamHandler(sys.stdout),
        ],
    )


def main():
    parser = argparse.ArgumentParser(description="arXiv OAI-PMH CS paper harvester")
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--new", nargs=2, metavar=("FROM_DATE", "UNTIL_DATE"), help="새 날짜 범위로 수집 (YYYY-MM-DD YYYY-MM-DD)")
    mode.add_argument("--continue", dest="cont", action="store_true", help="이전 수집이 끝난 날짜부터 오늘까지 이어서 수집")
    parser.add_argument("--db", default=DB_DSN, metavar="DSN", help=f"PostgreSQL DSN (default: {DB_DSN})")
    parser.add_argument("--no-resume", action="store_true", help="중단된 resumptionToken 무시하고 처음부터")
    parser.add_argument("--limit", type=int, default=MAX_RECORDS, metavar="N", help=f"최대 수집 건수 (default: {MAX_RECORDS}, 0=무제한)")
    args = parser.parse_args()

    setup_logging(LOG_FILE)
    log = logging.getLogger("main")

    resume = not args.no_resume

    if args.new:
        from_date, until_date = args.new
    else:
        # --continue: last_harvest_date 로드
        db_check = Database(args.db)
        state = db_check.load_harvest_state()
        db_check.close()
        if state is None or state.last_harvest_date is None:
            parser.error("이전 수집 기록이 없습니다. 먼저 --new FROM UNTIL 로 수집하세요.")
        from_date = state.last_harvest_date
        until_date = date.today().isoformat()
        log.info("이전 수집 완료일 %s 부터 오늘 %s 까지 수집", from_date, until_date)

    db = Database(args.db)
    client = OaiPmhClient(
        endpoint=OAI_ENDPOINT,
        rate_limit_secs=RATE_LIMIT_SECS,
        timeout=REQUEST_TIMEOUT,
        max_retries=MAX_RETRIES,
        retry_backoff=RETRY_BACKOFF,
    )
    oai_parser = OaiPmhParser()
    harvester = Harvester(
        client=client,
        parser=oai_parser,
        db=db,
        set_spec=OAI_SET,
        metadata_prefix=METADATA_PREFIX,
    )

    log.info("Starting harvest — set=%s db=%s from=%s until=%s resume=%s",
             OAI_SET, args.db, from_date, until_date, resume)
    try:
        max_records = None if args.limit == 0 else args.limit
        harvester.run(from_date=from_date, until_date=until_date, resume=resume, max_records=max_records)
    except KeyboardInterrupt:
        log.info("Interrupted by user. Progress saved — run again to resume.")
    finally:
        count = db.get_paper_count()
        log.info("Papers in DB: %d", count)
        db.close()


if __name__ == "__main__":
    main()
