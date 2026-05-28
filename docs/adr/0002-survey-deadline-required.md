# ADR-0002: `surveys.deadline`을 NOT NULL로, draft 생성 시점에 입력 강제

- 상태: Accepted
- 날짜: 2026-05-26
- 관련: M1 Foundation, `surveys` 테이블, `SurveyDraftReq`

## Context
초기 SPEC 검토 단계에서 `surveys.deadline`을 nullable로 두는 안이 있었다 ("임시 저장 시점엔 데드라인 미정인 케이스 허용"). 그러나 두 가지 문제가 보였다.
1. **publish 검증 누락 위험**: deadline이 비어 있는 draft가 그대로 publish 가능하면, 응답자에게 "마감 없음" 설문이 노출됨.
2. **분석 시점 cohort 모호**: M6 LLM 분석은 "deadline 기준으로 응답 cohort"를 정의. deadline이 없으면 분석 트리거 시점과 응답 종료 시점의 정합성이 깨짐.

## Decision
- `surveys.deadline TIMESTAMP NOT NULL`.
- `POST /api/admin/surveys/draft` 요청 DTO(`SurveyDraftReq`)에 `deadline` 필수 필드 포함.
- 입력 누락 시 400 `MISSING_DEADLINE`.

## Consequences
- ✅ 스키마 제약으로 invalid state 진입 불가 — 추가 검증 코드 최소화.
- ✅ M3 publish, M6 analyze 모두 deadline 존재를 가정할 수 있음.
- ❌ "데드라인 나중에 정하고 싶다"는 UX 요구가 생기면 별도 컬럼(`deadline_tentative` 등)이나 새 ADR 필요.
