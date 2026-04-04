import psycopg2
import psycopg2.extras
from datetime import datetime, timezone

from .models import ArxivRecord, HarvestState

SCHEMA_SQL = """
CREATE TABLE IF NOT EXISTS papers (
    arxiv_id         TEXT PRIMARY KEY,
    title            TEXT NOT NULL,
    abstract         TEXT,
    categories       TEXT NOT NULL,
    primary_category TEXT,
    license          TEXT,
    submitter        TEXT,
    created_date     TEXT,
    updated_date     TEXT,
    oai_identifier   TEXT UNIQUE,
    oai_datestamp    TEXT,
    is_deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    harvested_at     TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS authors (
    id          SERIAL PRIMARY KEY,
    arxiv_id    TEXT NOT NULL REFERENCES papers(arxiv_id) ON DELETE CASCADE,
    position    INTEGER NOT NULL,
    keyname     TEXT NOT NULL,
    forenames   TEXT
);

CREATE TABLE IF NOT EXISTS harvest_state (
    id                INTEGER PRIMARY KEY CHECK (id = 1),
    last_harvest_date TEXT,
    resumption_token  TEXT,
    total_harvested   INTEGER NOT NULL DEFAULT 0,
    last_updated_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_papers_created    ON papers(created_date);
CREATE INDEX IF NOT EXISTS idx_papers_updated    ON papers(updated_date);
CREATE INDEX IF NOT EXISTS idx_papers_datestamp  ON papers(oai_datestamp);
CREATE INDEX IF NOT EXISTS idx_authors_arxiv_id  ON authors(arxiv_id);
"""


class Database:
    def __init__(self, dsn: str):
        self.conn = psycopg2.connect(dsn)
        self.conn.autocommit = False
        self.create_schema()

    def create_schema(self):
        with self.conn.cursor() as cur:
            for statement in SCHEMA_SQL.split(";"):
                statement = statement.strip()
                if statement:
                    cur.execute(statement)
        self.conn.commit()

    def upsert_paper(self, record: ArxivRecord):
        now = datetime.now(timezone.utc)
        with self.conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO papers
                    (arxiv_id, title, abstract, categories, primary_category,
                     license, submitter, created_date, updated_date,
                     oai_identifier, oai_datestamp, is_deleted, harvested_at)
                VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
                ON CONFLICT (arxiv_id) DO UPDATE SET
                    title            = EXCLUDED.title,
                    abstract         = EXCLUDED.abstract,
                    categories       = EXCLUDED.categories,
                    primary_category = EXCLUDED.primary_category,
                    license          = EXCLUDED.license,
                    submitter        = EXCLUDED.submitter,
                    created_date     = EXCLUDED.created_date,
                    updated_date     = EXCLUDED.updated_date,
                    oai_identifier   = EXCLUDED.oai_identifier,
                    oai_datestamp    = EXCLUDED.oai_datestamp,
                    is_deleted       = EXCLUDED.is_deleted,
                    harvested_at     = EXCLUDED.harvested_at
                """,
                (
                    record.arxiv_id,
                    record.title,
                    record.abstract,
                    record.categories,
                    record.primary_category,
                    record.license,
                    record.submitter,
                    record.created_date,
                    record.updated_date,
                    record.oai_identifier,
                    record.oai_datestamp,
                    record.is_deleted,
                    now,
                ),
            )
            cur.execute("DELETE FROM authors WHERE arxiv_id = %s", (record.arxiv_id,))
            psycopg2.extras.execute_values(
                cur,
                "INSERT INTO authors (arxiv_id, position, keyname, forenames) VALUES %s",
                [(record.arxiv_id, a.position, a.keyname, a.forenames) for a in record.authors],
            )

    def upsert_papers_batch(self, records: list):
        for record in records:
            self.upsert_paper(record)
        self.conn.commit()

    def save_harvest_state(self, last_date: str | None, token: str | None, total: int):
        now = datetime.now(timezone.utc)
        with self.conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO harvest_state
                    (id, last_harvest_date, resumption_token, total_harvested, last_updated_at)
                VALUES (1, %s, %s, %s, %s)
                ON CONFLICT (id) DO UPDATE SET
                    last_harvest_date = EXCLUDED.last_harvest_date,
                    resumption_token  = EXCLUDED.resumption_token,
                    total_harvested   = EXCLUDED.total_harvested,
                    last_updated_at   = EXCLUDED.last_updated_at
                """,
                (last_date, token, total, now),
            )
        self.conn.commit()

    def load_harvest_state(self) -> HarvestState | None:
        with self.conn.cursor() as cur:
            cur.execute(
                "SELECT last_harvest_date, resumption_token, total_harvested FROM harvest_state WHERE id = 1"
            )
            row = cur.fetchone()
        if row is None:
            return None
        return HarvestState(
            last_harvest_date=row[0],
            resumption_token=row[1],
            total_harvested=row[2],
        )

    def get_paper_count(self) -> int:
        with self.conn.cursor() as cur:
            cur.execute("SELECT COUNT(*) FROM papers")
            return cur.fetchone()[0]

    def close(self):
        self.conn.close()
