# ADR-0006: LLM 호출은 `@Transactional` 바깥에서 + `LlmTransientException` → 503

- 상태: Accepted
- 날짜: 2026-05-26
- 관련: M4 Draft generation / M6 Analyze, `SurveyDraftService`, `SurveyService.createDraftWithQuestions`, 글로벌 예외 핸들러

## Context
초안 설계는 `SurveyDraftService.createDraft()`에 `@Transactional`을 걸고 그 안에서 Bedrock(Claude Haiku 4.5)을 호출했다. 두 가지 문제가 보였다.

1. **DB 커넥션 점유**: Bedrock 응답은 수 초 단위 지연이 가능. 그 시간 동안 트랜잭션이 열려 있어 커넥션 풀을 점유. 동시 사용자가 늘면 풀 고갈 위험.
2. **장애 분류**: Bedrock의 일시 장애(throttling, 5xx)를 HTTP 4xx로 그대로 노출하면 클라이언트가 retry 정책을 결정할 단서를 잃음.

## Decision
1. **`SurveyDraftService`의 `@Transactional` 제거**. LLM 호출은 트랜잭션 밖에서 실행.
2. **`SurveyService.createDraftWithQuestions(...)`** 메서드 신설 — 이쪽에만 `@Transactional` 부여. LLM 결과를 받아서 survey + questions를 한 트랜잭션에 일괄 저장.
3. **`LlmTransientException`** 신설. Bedrock 일시 장애(throttling, 5xx, timeout)를 이 예외로 wrap.
4. **글로벌 핸들러**: `LlmTransientException` → HTTP 503 + `code: LLM_TRANSIENT`. 영구 장애(API key 오류 등)는 500 그대로.

## Consequences
- ✅ DB 커넥션 점유 시간이 "LLM 호출 시간"이 아니라 "INSERT 몇 건" 시간으로 축소.
- ✅ 클라이언트가 503을 보고 retry 가능 (4xx와 명확히 구분).
- ❌ 부분 실패 시나리오: LLM 성공 → DB 실패. LLM 비용은 이미 발생, 사용자에겐 에러. 토이 규모에선 수용 (재시도하면 새 LLM 호출).
- 추후 idempotency key를 도입해 "LLM 결과 캐시 후 재시도"가 가능해지면 새 ADR로 supersedes.
