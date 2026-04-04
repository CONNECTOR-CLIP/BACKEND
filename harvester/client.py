import logging
import time

import requests

log = logging.getLogger(__name__)


class HarvesterError(Exception):
    pass


class OaiPmhClient:
    def __init__(self, endpoint: str, rate_limit_secs: float, timeout: int, max_retries: int, retry_backoff: list):
        self.endpoint = endpoint
        self.rate_limit_secs = rate_limit_secs
        self.timeout = timeout
        self.max_retries = max_retries
        self.retry_backoff = retry_backoff
        self.session = requests.Session()
        self.session.headers["User-Agent"] = "arXiv-OAI-Harvester/1.0 (research use)"
        self._last_request_time = 0.0

    def _wait_for_rate_limit(self):
        elapsed = time.monotonic() - self._last_request_time
        wait = self.rate_limit_secs - elapsed
        if wait > 0:
            log.debug("Rate limit: sleeping %.1fs", wait)
            time.sleep(wait)

    def _get(self, params: dict) -> str:
        for attempt in range(self.max_retries + 1):
            self._wait_for_rate_limit()
            log.debug("GET %s params=%s", self.endpoint, params)
            try:
                resp = self.session.get(self.endpoint, params=params, timeout=self.timeout)
                self._last_request_time = time.monotonic()

                if resp.status_code == 503:
                    retry_after = int(resp.headers.get("Retry-After", self.retry_backoff[min(attempt, len(self.retry_backoff) - 1)]))
                    log.warning("HTTP 503 — sleeping %ds before retry %d/%d", retry_after, attempt + 1, self.max_retries)
                    time.sleep(retry_after)
                    continue

                resp.raise_for_status()
                log.debug("Response: %d bytes", len(resp.content))
                return resp.text

            except requests.exceptions.ConnectionError as e:
                wait = self.retry_backoff[min(attempt, len(self.retry_backoff) - 1)]
                log.warning("ConnectionError (attempt %d/%d): %s — retrying in %ds", attempt + 1, self.max_retries, e, wait)
            except requests.exceptions.Timeout as e:
                wait = self.retry_backoff[min(attempt, len(self.retry_backoff) - 1)]
                log.warning("Timeout (attempt %d/%d): %s — retrying in %ds", attempt + 1, self.max_retries, e, wait)
            except requests.exceptions.HTTPError as e:
                raise HarvesterError(f"HTTP error: {e}") from e

            if attempt < self.max_retries:
                time.sleep(self.retry_backoff[min(attempt, len(self.retry_backoff) - 1)])

        raise HarvesterError(f"Failed after {self.max_retries} retries: {params}")

    def list_records(self, set_spec: str, metadata_prefix: str, from_date: str | None = None, until_date: str | None = None) -> str:
        params = {"verb": "ListRecords", "set": set_spec, "metadataPrefix": metadata_prefix}
        if from_date:
            params["from"] = from_date
        if until_date:
            params["until"] = until_date
        return self._get(params)

    def list_records_resumption(self, token: str) -> str:
        return self._get({"verb": "ListRecords", "resumptionToken": token})
