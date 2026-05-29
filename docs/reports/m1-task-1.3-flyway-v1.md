# 작업 보고서 — M1 Task 1.3: Flyway V1 (9개 테이블)

- **Milestone / Task:** M1 Foundation / Task 1.3
- **브랜치:** `feat/foundation`
- **날짜:** 2026-05-29

## 변경 파일 목록

| 구분 | 파일 | 핵심 변경 |
|------|------|-----------|
| 생성 | `src/main/resources/db/migration/V1__init.sql` | 9개 테이블 + 인덱스 + CHECK 제약 (PLAN 그대로) |

테이블: `admins`, `adoptions`, `surveys`, `questions`, `question_options`, `survey_responses`, `answers`, `analyses`, `action_items`

정책 반영:
- **DB 레벨 FK 제약 없음** — 무결성은 JPA/서비스 레이어. FK 컬럼엔 인덱스 유지.
- `surveys.slack_webhook_url` **미생성** (서버 config 단일 채널 정책, SSRF 차단).
- `surveys.deadline` **NOT NULL** (draft 생성 시 필수).
- `answers` — `text_value`/`question_option_id`/`scale_value` 중 **정확히 1개**만 non-null (CHECK).
- `analyses.positive_signals`/`resistance_factors`/`risks` = **JSONB**.

## 검증 + 셀프 리뷰

검증 방법: postgres 컨테이너(`docker compose`) 내부 psql로 V1 SQL을 **트랜잭션 적용 → 검증 쿼리 → ROLLBACK** (DB 무오염 유지).

| 항목 | 기대 | 실제 |
|------|------|------|
| `CREATE TABLE`/`CREATE INDEX` 실행 | 에러 없음 | ✅ 9 테이블 + 인덱스 전부 성공 |
| `information_schema.tables` count | 9 | ✅ 9 |
| `pg_indexes` count | 21 (9 PK + 3 UNIQUE + 9 명시) | ✅ 21 |
| `answers` CHECK 제약 | 존재 | ✅ `answers_check` |
| `adoptions` CHECK 제약 | 존재 | ✅ `adoptions_target_count_check` |
| `analyses` JSONB 컬럼 | 3 (positive_signals/resistance_factors/risks) | ✅ 3 |

셀프 리뷰: SQL 작업(트리비얼) → 인라인 리뷰만. SPEC/PLAN 스키마와 컬럼·타입·제약 대조 일치. `/code-review` 미실행 (로직 아님).

## 결정 / 이탈 사항

- **검증 방법 이탈**: PLAN Step 2는 `bootRun` + 호스트 `psql`로 검증하나 — ① 호스트에 `psql` 없음, ② `bootRun`은 Spring AI Bedrock 오토컨피그가 시작 시 AWS region을 요구(현재 `spring.ai.bedrock.*` 미설정)해 마이그레이션과 무관하게 실패 가능. 따라서 컨테이너 psql + 트랜잭션/롤백으로 SQL 유효성·스키마를 직접 검증. **실제 Flyway 파이프라인(flyway_schema_history 생성 포함) 검증은 Testcontainers가 도입되는 Task 1.4로 이연.**
- SQL 내용은 PLAN 코드 블록 그대로 — 이탈 없음.
- ADR 트리거 없음.
- 비고: 검증용으로 띄운 `compose` postgres 컨테이너는 종료하지 않고 실행 중(로컬 dev DB, 무오염 빈 상태). 불필요 시 `docker compose down -v`.
