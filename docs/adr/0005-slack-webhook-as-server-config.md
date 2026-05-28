# ADR-0005: Slack Webhook URL은 서버 config로 단일 고정

- 상태: Accepted
- 날짜: 2026-05-26
- 관련: M3 Survey publication, `surveys` 테이블, `SurveyPublishReq`(삭제됨)

## Context
초기 SPEC은 `surveys.slack_webhook_url VARCHAR(...)` 컬럼을 두어 설문별 webhook 지정을 허용하려 했다. 발행(publish) API도 본문에 `slack_webhook_url`을 받는 `SurveyPublishReq` DTO를 노출했다.

실제 운영 모델을 재확인한 결과:
- AdoptLoop의 토이 운영자는 1명.
- 모든 설문의 발행 알림이 동일한 채널로 가는 게 자연스러움.
- per-survey webhook은 multi-tenant 시점에야 의미 있음.

이 상태로 두면 (a) 사용 안 하는 컬럼이 스키마에 잔존, (b) publish API가 불필요한 body를 받아 검증·문서화 부담이 늘어남.

## Decision
- `surveys.slack_webhook_url` 컬럼 제거 (Flyway migration).
- `application.yml`에 `adoptloop.slack.webhook-url`(단일 값) 설정. 환경별 override는 Spring profile로.
- `SurveyPublishReq` DTO 삭제. `POST /api/admin/surveys/{id}/publish`는 body 없는 POST.
- 발행 시 서버는 config의 webhook URL을 읽어 알림 전송.

## Consequences
- ✅ 스키마·DTO·API 세 곳 동시 단순화. 변경 한 곳(config) = 운영 변경 한 곳.
- ✅ webhook URL이 DB 평문에 남지 않음 (보안 측면 부수 이득).
- ❌ multi-tenant 또는 "설문별 다른 채널" 요구가 생기면 다시 컬럼화 필요.
- **미래 supersedes 조건**: 위 요구가 발생하면 새 ADR을 작성해 본 ADR을 `Superseded by ADR-XXXX`로 표시. 컬럼·DTO를 다시 도입하는 마이그레이션을 그 ADR에 명시.
