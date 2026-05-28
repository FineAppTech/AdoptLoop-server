# ADR Index

> Architecture Decision Records — AdoptLoop의 SPEC/TECH_STACK 편집급 결정 기록.
> 새 ADR 작성 규칙은 `docs/PROJECT_CONTEXT.md` 참조.

| # | 제목 | 상태 | 한 줄 요약 |
|---|---|---|---|
| [0001](0001-enum-uppercase-convention.md) | Enum UPPERCASE 컨벤션 | Accepted | DB DEFAULT / JSON wire / LLM 프롬프트까지 일관 |
| [0002](0002-survey-deadline-required.md) | Survey deadline NOT NULL | Accepted | draft 생성 시점에 입력 강제 |
| [0003](0003-jackson-snake-case-and-dto-naming.md) | Jackson SNAKE_CASE 전역 + DTO 네이밍 | Accepted | `@JsonProperty` 제거, `Req`/`Res`/`Vo`/`Dto` 컨벤션 |
| [0004](0004-error-mapping-via-response-status-exception.md) | 에러 매핑 `ResponseStatusException` 단일화 | Accepted | `IllegalArgumentException`/`IllegalStateException` 자동 매핑 제거 |
| [0005](0005-slack-webhook-as-server-config.md) | Slack Webhook URL 서버 config 고정 | Accepted | `surveys.slack_webhook_url` 컬럼 제거, `SurveyPublishReq` 삭제 |
| [0006](0006-llm-call-outside-transaction-and-503-mapping.md) | LLM 호출 트랜잭션 분리 + 503 매핑 | Accepted | DB 커넥션 점유 방지, `LlmTransientException` 도입 |
| [0007](0007-response-validation-policy.md) | 응답 검증 정책 | Accepted | required 누락 거부, `target_count*10` cap, 빈 TEXT 거부 |
| [0008](0008-ecs-deployment-latest-tag-force-new.md) | ECS 배포 `:latest` + `force-new-deployment` | Accepted | 토이 우선 — production hygiene 의도적 보류 |
