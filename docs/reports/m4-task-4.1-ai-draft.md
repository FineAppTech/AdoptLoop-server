# 작업 보고서 — M4 Task 4.1: AI 설문 초안

- **Milestone / Task:** M4 AI 설문 초안 / Task 4.1
- **브랜치:** `feat/ai-draft` (base: `b7fbca5` — PR #7/M3 머지본)
- **날짜:** 2026-05-29
- **성격:** 로직 → `/code-review`(medium, finder 3종 병렬 + triage) 수행

## 개요

도입 정보 → Bedrock(Claude Haiku 4.5) Converse로 설문 JSON 생성 → 파싱 → `createDraftWithQuestions`로 draft + 질문 원자 저장. 엔드포인트 `POST /api/admin/adoptions/{adoptionId}/surveys`.

## 변경 파일 목록 (8파일, +358)

| 구분 | 파일 | 핵심 |
|------|------|------|
| 생성 | `config/BedrockConfig.kt` | `BedrockProxyChatModel` → `ChatClient` 빈 |
| 생성 | `survey/draft/SurveyDraftPrompt.kt` | 도입정보 → 한국어 설문 설계 프롬프트(SCALE 축별 ≥1, JSON-only) |
| 생성 | `survey/draft/SurveyDraftParser.kt` | LLM JSON → `SurveyDraftPayload`(Jackson 3 tree API). 머리말 섞여도 `{`~`}` 추출. **빈 값/빈 질문 가드** |
| 생성 | `survey/draft/SurveyDraftService.kt` | LLM 호출(`@Transactional` 밖) → 파싱 → 원자 저장. 실패는 `LlmTransientException`(503), 원인 로깅 |
| 수정 | `survey/SurveyController.kt` | `generateDraft` 엔드포인트(201) 추가, `draftService` 주입 |
| 수정 | `survey/SurveyDtos.kt` | `SurveyDraftReq.deadline`에 `@field:Future` (LLM 호출 전 fail-fast) |
| 생성 | `test/survey/SurveyDraftServiceTest.kt` | 생성(201)/LLM실패(503)/과거마감(400+LLM미호출) + `documentApi`×3 |
| 생성 | `test/survey/draft/SurveyDraftParserTest.kt` | 파서 단위 테스트 4종(유효/JSON없음/빈질문/title null) |

## 검증 (GREEN)

1. **컴파일**: main 소스 전부 GREEN. PLAN의 `com.fasterxml.jackson.*` → **`tools.jackson.*`**, `.asText()` → `.asString()`(Jackson 3.1.2 정식) 정정. ChatClient fluent API(`prompt().user().call().content()`) 정상.
2. **전체 스위트**: BUILD SUCCESSFUL — **20 tests / 0 fail / 0 err** (M1·M2·M3 13 + M4 7). 회귀 0.
3. **REST Docs 스니펫 3종**: `generate-survey-draft`(요청/응답 필드), `generate-survey-draft-llm-error`(503), `generate-survey-draft-invalid-deadline`(400). `documentApi` 강제(ADR-0009) 충족.

### 테스트 케이스

| 테스트 | 검증 |
|------|------|
| `LLM JSON으로 설문 초안을 생성한다` | mock ChatClient → **201**, title/status=DRAFT, questions=2, options=2 (happy-path) |
| `LLM 호출이 실패하면 503` | ChatClient 예외 → **503 code=LLM_TRANSIENT** |
| `마감이 과거면 400이고 LLM을 호출하지 않는다` | `@field:Future` → **400 VALIDATION_FAILED**, `verify(exactly=0){prompt()}` (fail-fast) |
| 파서: 유효/JSON없음/빈질문/title=null | 본문 추출·옵션 orderIndex / 나머지 3종 예외(→서비스가 503으로 래핑) |

## 사전 검증으로 확인한 사실 (PLAN 정정·리스크 해소)

- **`LlmTransientException` + 503 핸들러는 이미 존재**(config/GlobalExceptionHandler) → 신규 생성 불필요.
- **Bedrock autoconfig는 AWS env 없이도 안전**: `BedrockAwsConnectionProperties.region` 기본값 `us-east-1` → 모델 빈 생성 시 `regionProvider.getRegion()` 성공. 실제 호출은 테스트에서 발생 안 함(ChatClient는 `@MockkBean`). → **초기에 시도한 테스트 프로파일 autoconfig exclude는 불필요했고 회귀를 유발**(BedrockConfig.chatClient가 모든 컨텍스트에서 모델 빈을 요구). 되돌림. `application-test.yaml` net 무변경.
- **springmockk `@MockkBean`은 Boot 4에서 정상 동작**. PLAN의 import `com.ninja_squad.springmockk`는 오타 — 실제 패키지는 **`com.ninjasquad.springmockk`**(언더스코어 없음).
- REST Docs: 빈 `options:[]` 배열은 배열 필드 `questions[].options` descriptor 없이는 미문서화로 실패 → 배열 필드 한 줄 추가(M3는 옵션이 항상 비어 통과했던 케이스).

## /code-review (medium) — finder 3종 + triage

| finder 지적 | 판정 | 조치 |
|------|------|------|
| 빈 `questions:[]` → 발행 불가한 0문항 draft가 201 | **수정** | 파서 `require(questions.isNotEmpty())` → 503 |
| JSON `null` title/text가 가드 통과(NullNode.asString()=`""`) | **수정** | `?.asString()?.takeIf(String::isNotBlank)` (디컴파일로 `""` 확인) |
| 과거 deadline이 LLM 호출 *후* 검증됨(비용 낭비) | **수정** | `@field:Future` → LLM 호출 전 400 |
| LLM 실패 원인이 어디에도 로깅 안 됨(503 관측성) | **수정** | `log.warn(cause)` 양쪽 catch |
| `DraftPayloadDto`만 `*Dto` 접미사(컨벤션은 Req/Res/Vo) | **수정** | `SurveyDraftPayload`로 개명 |
| 소유권 체크 이중(generate + createDraft) | **수용** | 조기 체크는 LLM 전 fail-fast + 프롬프트용 adoption 확보 목적. inner는 SurveyService 자체 불변식. M3의 "AdoptionService.get 중복 추상화 시기상조" 선례와 동일 |
| 옵션이 비배열(객체/스칼라)이면 garble/drop | **수용** | 프롬프트가 배열 예시 명시·발생 시 503/이상데이터. 토이 규모 over-hardening 회피 |
| 저장 시 `DataIntegrityViolation`(슬러그 충돌 등) → 미매핑 500 | **수용** | 12바이트 랜덤 슬러그 충돌은 사실상 불가(96bit). 예기치 못한 DB 오류의 500은 타당 — 전역 핸들러 후속(M4 비국소) |
| `BedrockConfig`가 concrete `BedrockProxyChatModel` 주입·fallback 없음 | **기각** | PLAN 설계대로. 운영선 autoconfig가 제공. Builder 충돌 없음(빈 타입=ChatClient) |

## 결정 / 이탈 사항

- **PLAN 정정**: 파서 import `tools.jackson`/`asString()`(Jackson 3), springmockk 패키지명 오타, autoconfig exclude 불필요(되돌림).
- **PLAN 대비 보강(의도적)**: PLAN은 happy-path 테스트 1개만 명시 → 503·400 컨트롤러 테스트 + 파서 단위 테스트 4종 추가(goal-driven: 유효하지 않은 입력/LLM 실패 경로 검증). 파서 가드·`@field:Future`·실패 로깅도 리뷰 발견을 반영한 보강.
- **M3 이월 `@field:Valid`(QuestionReq.options)는 M4 범위에서 제외**: M4 생성 경로는 옵션을 LLM 파서가 만들어 넣어 request-body `@Valid`를 타지 않음(원 이월 근거가 어긋남). 옵션 선택이 1급이 되는 M5 또는 별도 소규모 변경으로 처리.
- **트랜잭션 경계**: `SurveyDraftService`는 비트랜잭션 — LLM 호출이 DB 트랜잭션 밖. `createDraftWithQuestions`(다른 빈, `@Transactional`)가 draft+질문을 원자 커밋(self-invocation 아님). 컨트롤러 `toDetail` 재조회는 별도 readOnly 트랜잭션·엔티티는 스칼라 FK라 lazy 이슈 없음.
- **네이밍**: `a`→`adoption`, `om`→`objectMapper`, `adoptionRepo`→`adoptionRepository`(약어 금지). 테스트 idiom `mvc` 유지.
- ADR 트리거 없음.
