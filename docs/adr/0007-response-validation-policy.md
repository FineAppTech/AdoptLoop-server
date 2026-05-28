# ADR-0007: 응답 검증 정책 — required / cap / 빈 텍스트

- 상태: Accepted
- 날짜: 2026-05-26
- 관련: M5 Response collection, `POST /api/public/surveys/{access_token}/responses`

## Context
응답자 API는 **인증 없는 public 엔드포인트**다. 두 가지 동시 요구.
1. **데이터 품질**: required 질문 누락이나 빈 텍스트가 분석 노이즈로 흘러들어가면 안 됨.
2. **Abuse 1차 방어**: 토이라 정교한 rate limit·CAPTCHA를 두기 어려움. 그러나 무방어면 한 명이 수만 건을 쓰면 DB·LLM 비용이 폭주.

## Decision
- **Required 질문 누락 → 400 `MISSING_REQUIRED`**. 클라이언트 수정으로 해결 가능한 에러.
- **응답 수 cap = `surveys.target_count * 10`**. 초과 시 403 `RESPONSE_CAP_EXCEEDED`. 정상 응답률을 매우 후하게 잡아도 안전 마진.
- **TEXT 응답에서 `trim()` 후 빈 문자열 → 400 `EMPTY_TEXT_RESPONSE`**.

## Consequences
- ✅ Abuse 한 명이 무한히 채우는 시나리오 차단 (cap).
- ✅ M6 분석에 빈 문자열/누락 데이터가 들어오지 않음.
- ❌ cap이 너무 빡빡하면 정상 트래픽도 차단될 수 있음. 운영 데이터를 보고 배수 조정 필요 — 그땐 본 ADR 업데이트 또는 새 ADR.
- ❌ 분산 환경에서 cap 카운트의 race condition 가능 — DB COUNT를 트랜잭션 안에서 잡으면 충분, 정확한 atomic counter는 과함.
