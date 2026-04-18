# arXiv OAI-PMH 수집기 알고리즘 리포트

## 전체 흐름

```
main.py
  └─ Harvester.run()
       ├─ OaiPmhClient  → arXiv 서버에 HTTP 요청
       ├─ OaiPmhParser  → XML 응답 파싱
       └─ Database      → SQLite 저장 + 상태 관리
```

---

## 1. 진입점 (main.py)

실행 모드는 두 가지 중 하나를 반드시 선택해야 합니다.

| 옵션 | 동작 |
|---|---|
| `--new FROM UNTIL` | 지정한 날짜 범위로 새로 수집 |
| `--continue` | DB에 저장된 `last_harvest_date`부터 오늘까지 자동 수집 |

공통 옵션:

| 옵션 | 기본값 | 설명 |
|---|---|---|
| `--limit N` | 100 | 최대 수집 건수 (0=무제한) |
| `--no-resume` | False | 중단된 토큰 무시, 처음부터 재수집 |
| `--db PATH` | arxiv_cs_ai.db | SQLite 파일 경로 |

---

## 2. HTTP 요청 (client.py)

### 요청 제한
매 요청 전 `time.monotonic()`으로 경과 시간을 측정해 arXiv 정책인 **5초 간격**을 강제합니다.

### 최초 요청 vs 페이지네이션 요청
```
최초 요청:
  GET /oai?verb=ListRecords&set=cs&metadataPrefix=arXiv&from=...&until=...

페이지네이션 요청 (resumptionToken 있을 때):
  GET /oai?verb=ListRecords&resumptionToken=xxxxx
  ※ token 외 다른 파라미터 없음 (서버가 이미 알고 있음)
```

### 재시도 로직
오류 상황별로 다음과 같이 처리합니다.

| 오류 | 처리 |
|---|---|
| `ConnectionError` / `Timeout` | 최대 5회 재시도, 대기 시간: 5→15→30→60→120초 |
| HTTP 503 | `Retry-After` 헤더 값만큼 대기 후 재시도 |
| 기타 HTTP 4xx/5xx | 즉시 예외 발생, 중단 |

---

## 3. XML 파싱 (parser.py)

arXiv OAI-PMH 응답은 두 가지 XML 네임스페이스가 중첩된 구조입니다.

```xml
<OAI-PMH>                          ← NS_OAI
  <ListRecords>
    <record>
      <header>
        <identifier>oai:arXiv.org:2501.12345</identifier>
        <datestamp>2025-01-15</datestamp>
      </header>
      <metadata>
        <arXiv>                    ← NS_ARXIV
          <title>...</title>
          <authors>
            <author>
              <keyname>LeCun</keyname>
              <forenames>Yann</forenames>
            </author>
          </authors>
          <abstract>...</abstract>
          <categories>cs.AI cs.LG</categories>
        </arXiv>
      </metadata>
    </record>
    <resumptionToken>abc123</resumptionToken>
  </ListRecords>
</OAI-PMH>
```

### 파싱 단계
1. `ET.fromstring()` — XML 문자열 → 트리 변환 (실패 시 빈 결과 반환, 수집 계속)
2. `<error>` 태그 확인 — OAI 에러 코드 추출 후 예외 발생
3. `<record>` 순회 — 각 논문을 `ArxivRecord` 객체로 변환
4. `<resumptionToken>` 추출 — 빈 문자열이면 마지막 페이지로 판단

### abstract 정규화
arXiv abstract에는 줄바꿈이 포함되어 있어 `\s+` 정규식으로 단일 공백으로 정규화합니다.

### 삭제 논문 처리
`<header status="deleted">` 인 경우 메타데이터 없이 `is_deleted=True`로만 저장합니다 (물리 삭제 하지 않음).

---

## 4. 수집 루프 (harvester.py)

```
시작
 │
 ├─ harvest_state 로드
 │
 ├─ resumption_token 있음? ──Yes──→ 해당 토큰으로 재개
 │         │
 │        No
 │         │
 │         └──→ from_date로 최초 요청
 │
 └─ [ 루프 시작 ]
       │
       ├─ 1. arXiv에 요청 (토큰 or 날짜 파라미터)
       │
       ├─ 2. XML 파싱 → records 추출
       │
       ├─ 3. limit 초과분 잘라내기
       │      records = records[:remaining]
       │
       ├─ 4. DB 배치 upsert (단일 트랜잭션)
       │
       ├─ 5. harvest_state 저장 (체크포인트)
       │      last_harvest_date, resumption_token, total
       │
       └─ 6. 종료 조건 확인
              ├─ is_final_page == True  → last_harvest_date 업데이트 후 종료
              ├─ total >= max_records   → 종료
              └─ 아니면 루프 반복
```

### 에러 처리
| OAI 에러 코드 | 처리 |
|---|---|
| `badResumptionToken` | 토큰 만료 → 토큰 폐기 후 from_date로 재시작 |
| `noRecordsMatch` | 해당 기간 논문 없음 → 정상 종료 |

---

## 5. DB 저장 (database.py)

### 테이블 구조
```
papers (arxiv_id PK)
  └─ authors (arxiv_id FK, 1:N)

harvest_state (id=1 고정, 싱글톤)
```

### upsert 전략
같은 `arxiv_id`가 다시 오면 덮어씁니다 (`INSERT OR REPLACE`).  
저자는 논문 upsert 시 기존 행 전체 삭제 후 재삽입합니다.

### 트랜잭션
배치 1000건 전체를 `with self.conn:` 블록으로 묶어 단일 커밋합니다.  
중간 실패 시 해당 배치 전체가 롤백되어 부분 저장이 발생하지 않습니다.

### SQLite 설정
```sql
PRAGMA journal_mode=WAL   -- 읽기/쓰기 동시 접근 허용
PRAGMA foreign_keys=ON    -- authors.arxiv_id → papers.arxiv_id 제약 강제
```

---

## 6. 상태 관리 (harvest_state 테이블)

수집 진행 상태를 DB에 저장해 중단/재개와 증분 수집을 구현합니다.

| 컬럼 | 역할 |
|---|---|
| `last_harvest_date` | `--continue` 실행 시 이 날짜부터 시작 |
| `resumption_token` | 중단 시 저장 → 재실행 시 이 토큰으로 재개 |
| `total_harvested` | 누적 수집 건수 |

### 체크포인트 타이밍
```
배치 저장 완료 → 즉시 토큰 저장 → 다음 배치 요청
                      ↑
               Ctrl+C가 여기서 와도
               직전 배치는 이미 커밋됨
```

---

## 7. 데이터 모델 (models.py)

```python
ArxivRecord
  ├─ arxiv_id, oai_identifier, oai_datestamp, is_deleted
  ├─ title, abstract, categories, primary_category
  ├─ license, submitter, created_date, updated_date
  └─ authors: List[Author]

Author
  └─ position, keyname, forenames

ParsedResponse
  └─ records, resumption_token, is_final_page

HarvestState
  └─ last_harvest_date, resumption_token, total_harvested
```

---

## 8. 설정값 요약 (config.py)

| 설정 | 값 | 설명 |
|---|---|---|
| `OAI_ENDPOINT` | `https://oaipmh.arxiv.org/oai` | arXiv OAI-PMH 엔드포인트 |
| `OAI_SET` | `cs` | CS 전체 분야 |
| `METADATA_PREFIX` | `arXiv` | 구조화된 메타데이터 포맷 |
| `RATE_LIMIT_SECS` | `5.0` | 요청 간격 (arXiv 정책) |
| `MAX_RETRIES` | `5` | 최대 재시도 횟수 |
| `MAX_RECORDS` | `100` | 기본 수집 건수 제한 |
