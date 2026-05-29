# 작업 보고서 — M3 Task 3.1: Surveys 편집·발행·Slack

- **Milestone / Task:** M3 Surveys 편집·발행·Slack / Task 3.1
- **브랜치:** `feat/surveys`
- **날짜:** 2026-05-29
- **성격:** 로직 → `/code-review`(medium) 실행

## 변경 파일 목록

| 구분 | 파일 | 핵심 |
|------|------|------|
| 생성 | `survey/SurveyDtos.kt` | QuestionReq/OptionReq(@NotBlank) · SurveyDraftReq(M4용) · QuestionVo/OptionVo/SurveyRes(+`from`) · SurveyDetailRes |
| 생성 | `survey/SurveyService.kt` | replaceQuestions(PUT 전치환)·publish·detail·loadOptions + createDraft/createDraftWithQuestions(M4 Step5 호출용 pre-built), 소유자 검증, @Transactional |
| 생성 | `survey/SurveyController.kt` | `/api/admin` GET/surveys/{id} · PUT/{id}/questions · POST/{id}/publish(200) |
| 생성 | `survey/publish/SlackNotifier.kt` | config webhook으로 RestClient POST, URL 비면 skip, 예외는 warn 로깅 |
| 생성 | `survey/publish/SurveyPublisher.kt` | TX afterCommit 훅 — 커밋 후에만 Slack 전송(롤백 시 미전송) |
| 생성 | `test/survey/SurveyControllerTest.kt` | 빈 draft 발행 409 / PUT 전치환 / 발행 happy-path + `documentApi`(ADR-0009) |

## 검증 (GREEN)

1. **부팅 의존성 오류**: 최초 테스트 실행 시 `NoSuchBeanDefinitionException: RestClient$Builder`. Spring Boot 4.0에서 `RestClient.Builder` 빈이 자동 구성되지 않음(앱 부팅도 동일하게 실패할 사안). → `SlackNotifier`를 `RestClient.create()` 직접 생성으로 변경(PLAN의 builder 주입 정정). 의존성 1개 감소.
2. **GREEN**: `SurveyControllerTest` 3개 통과. 전체 스위트 BUILD SUCCESSFUL — **13 tests / 0 fail / 0 err**, M1·M2 회귀 0.
3. REST Docs 스니펫 3종 생성: `publish-survey-empty-draft-conflict`, `replace-survey-questions`(요청/응답 필드), `publish-survey`(응답 필드). `documentApi` 추적(강제) 정상.

### 테스트 케이스

| 테스트 | 검증 |
|------|------|
| `질문 없는 draft를 발행하면 409` | 발행 가드(질문 ≥1) → **409 code=CONFLICT** |
| `PUT으로 질문을 전치환한다` | TEXT+SCALE 2문항 교체 → **200**, `questions.length()==2` |
| `질문이 있는 draft를 발행한다` | 질문 추가 후 발행 → **200 status=PUBLISHED**, `published_at` 존재 (핵심 기능 happy-path) |

## /code-review (medium, 2 finder 병렬 + triage)

| finder 주장 | 판정 | 근거 |
|------|------|------|
| `tools.jackson.databind.ObjectMapper` import 오류(→fasterxml) | REFUTED | Boot 4.0=Jackson 3, `tools.jackson`이 정답. M2 테스트 동일·빌드 GREEN |
| `SurveyPublisher` registerSynchronization 트랜잭션 미확인 | REFUTED | `publish()`는 클래스 레벨 `@Transactional` → 항상 활성 TX |
| publish 시 explicit save() 누락 | REFUTED | `@Transactional` dirty-checking 관용 |
| `id!!` NPE | REFUTED | 영속 엔티티 id 항상 존재(M2 `from` 컨벤션과 동일) |
| `RestClient.create()` 인스턴스별 풀 누수 | REFUTED | `@Component` 싱글톤 1개 |
| 테스트 snake_case 맵 키가 Jackson 설정 의존 | REFUTED | 전역 SNAKE_CASE 계약을 의도적으로 검증 |
| `replaceQuestions` 두 번 DELETE 비효율 | 수용 | DB FK 미사용 정책상 cascade 불가, 토이 규모 |
| `requireAdoptionOwned` ↔ `AdoptionService.get` 중복 | 수용 | 호출처 2곳, 공통 베이스 도입은 시기상조 |
| `createDraft`/`createDraftWithQuestions`/`newSlug` 미호출 | **유지(계획대로)** | M4 Step5/6가 구체적으로 호출 → speculative 아님. 제거 시 PLAN 모순·M4 재작업 발생(사용자 결정) |

## 결정 / 이탈 사항

- **PLAN 정정(SlackNotifier)**: `RestClient.Builder` 주입 → `RestClient.create()`. Boot 4.0에 해당 빈 자동구성 없음. (M4 Bedrock `ChatClient`는 별도 경로라 영향 없음.)
- **PLAN 준수(pre-built 코드 유지)**: `SurveyService.createDraft`/`createDraftWithQuestions`/`newSlug` + `SurveyDraftReq`는 M3 엔드포인트·테스트는 없으나 **M4 Step5/6에서 구체적으로 호출**되는 pre-built 코드라 PLAN대로 유지(이탈 아님). 셀프리뷰의 '미호출' 지적은 한 번 제거했다가 검토 후 **유지로 결론(사용자 결정)** — 제거 시 PLAN 모순·M4 재작업 발생. M3 미커버는 의도적(M4에서 테스트 커버).
- **M4 이월 항목**: `QuestionReq.options`에 `@field:Valid` 누락(중첩 검증). 옵션은 SINGLE_CHOICE(M4)에서 1급 시민이 되므로 테스트와 함께 M4에서 추가.
- **DTO 컨벤션**: 응답 DTO는 M2 `AdoptionRes`와 동일하게 `companion object { fun from(...) }` 패턴 채택(PLAN의 컨트롤러 private `toView`/`toDetail` 대신, 단 `toDetail` 조립부는 컨트롤러 유지).
- **네이밍**: `surveyRepo`→`surveyRepository`, `optionRepo`→`optionRepository`, `props`→`slackProperties` (약어 금지 규칙). 테스트 idiom `mvc`/`objectMapper` 유지.
- ADR 트리거 없음.
