# 작업 보고서 — M5 Task 5.1: 응답자 공개 API

- **Milestone / Task:** M5 응답자 공개 API / Task 5.1
- **브랜치:** `feat/public-api` (base: `d904dad` — PR #8/M4 머지본)
- **날짜:** 2026-05-29
- **성격:** 로직 → `/code-review`(medium, finder 3종 병렬 + triage) 수행

## 개요

공개 slug로 설문을 열람하고, 토큰을 발급받아 답변을 PUT 전치환(replace-all) 제출하는 미인증 응답자 플로우. 엔드포인트 4종 `/api/public/*`. AdminKeyFilter는 `shouldNotFilter`가 `/api/admin` 외 경로를 통과시키므로 키 불필요.

| 메서드 | 경로 | 동작 | 음성 |
|--------|------|------|------|
| GET | `/surveys/{slug}` | 공개 설문 조회 | DRAFT→404, 마감지남→410 |
| POST | `/surveys/{slug}/responses` | 토큰 발급(201) | 응답 상한(targetCount×10)→429 |
| GET | `/responses/{token}` | 응답 재로드 | 토큰 오류→401 |
| PUT | `/responses/{token}/answers` | 답변 전치환 제출 | 토큰401·마감403·필수누락/타입·선택지오류400 |

## 변경 파일 목록 (4파일 신규)

| 구분 | 파일 | 핵심 |
|------|------|------|
| 생성 | `publicapi/PublicDtos.kt` | `PublicSurveyRes`/`ResponseTokenRes`/`AnswerReq`/`PublicResponseRes` |
| 생성 | `publicapi/PublicResponseService.kt` | `@Transactional`. slug/token 로드, 토큰 발급(SecureRandom 24B Base64url), PUT 전치환 제출, 마감·필수·타입·**선택지 멤버십** 검증 |
| 생성 | `publicapi/PublicSurveyController.kt` | 4 endpoint. 뷰 조립은 `QuestionVo.from`/`OptionVo.from` 팩토리 재사용 |
| 생성 | `test/publicapi/PublicSurveyControllerTest.kt` | 4 테스트 + `documentApi`×5 (start/submit happy + 401/410/선택지400) |

## 검증 (GREEN)

1. **전체 스위트**: BUILD SUCCESSFUL — **24 tests / 0 fail / 0 err** (M1~M4 20 + M5 4). 회귀 0.
2. **REST Docs 스니펫 5종**: `start-public-response`, `submit-public-response`(요청/응답 필드), `submit-public-response-invalid-token`(401), `submit-public-response-invalid-option`(400), `get-public-survey-deadline-passed`(410). `documentApi` 강제(ADR-0009) 충족.

### 테스트 케이스

| 테스트 | 검증 |
|------|------|
| `토큰 발급 후 답변을 제출하면 재로드 시 제출된 답변이 보인다` | start(201)→PUT(200, status=SUBMITTED, answers[0].text_value=good) 순차, 실제 커밋 가시성 |
| `잘못된 토큰으로 제출하면 401` | 미존재 토큰 → 401 (UNAUTHORIZED) |
| `마감이 지난 설문 조회는 410` | 과거 deadline GET → 410 (GONE) |
| `해당 문항에 속하지 않는 선택지로 제출하면 400` | SINGLE_CHOICE에 미존재 옵션 ID → 400 (멤버십 검증, 코드리뷰 발견 반영) |

## 사전 검증으로 확인한 사실 (PLAN 정정)

- **의존 API 전부 실재**: `SurveyRepository.findByPublicSlug`, `SurveyResponseRepository.findByAccessToken`/`countBySurveyId`, `AnswerRepository.deleteAllBySurveyResponseId`/`findAllBySurveyResponseId`, `QuestionRepository.findAllBySurveyIdOrderByOrderIndex`, `QuestionOptionRepository.findAllByQuestionIdInOrderByOrderIndex` — 시그니처 일치. 엔티티 생성자도 PLAN 테스트와 일치.
- **트랜잭션 가시성**: `IntegrationTestBase`는 롤백이 아닌 실제 TRUNCATE + 실제 커밋 → start→submit→reload 다중 HTTP 호출 간 데이터 보임.
- **AdminKeyFilter**: `shouldNotFilter`가 `/api/admin` 외 통과 → `/api/public/*` 키 불필요. ✓
- **GlobalExceptionHandler**: `ResponseStatusException` → `ErrorRes(code=HttpStatus.name, message=reason)`. 404/410/401/403/429/400 전부 매핑(미매핑 500 없음).
- **snake_case 전역**: `application.yaml property-naming-strategy: SNAKE_CASE` → `access_token`/`question_id`/`text_value` 등 직렬화·역직렬화 일치.

## /code-review (medium) — finder 3종 + triage

| finder 지적 | 판정 | 조치 |
|------|------|------|
| **SINGLE_CHOICE `questionOptionId` 멤버십 미검증** | **수정** | `answers.question_option_id`에 FK 없음(V1__init.sql `BIGINT`만, CHECK는 exactly-one-non-null만) → 미존재/타 문항 옵션이 그대로 저장되어 M6 집계 오염. 미인증 공개 입력. `submit`에서 문항별 유효 옵션 ID 집합 로드 후 멤버십 검증 추가 + 음성 테스트 |
| 재제출 시 `submittedAt` 미갱신 | 수용 | 의도(=최초 제출시각). 수정시각은 `@LastModifiedDate updatedAt`이 포착 |
| 에러가 required/unknown question ID 노출 | 기각 | `GET /api/public/surveys/{slug}`이 이미 전 문항 ID·required 공개 → 증분 누출 0 |
| submit→load stale-read | 기각 | Postgres 최소 격리=READ_COMMITTED, 두 호출은 별도 top-level 트랜잭션(컨트롤러 비트랜잭션)이라 submit 커밋 후 load |
| 'draft survey' 메시지가 발행상태 노출 | 수용 | 두 경로 동일 HTTP 404, 메시지만 상이·비민감(토이) |
| `PublicResponseRes.answers`가 `AnswerReq`(Req) 재사용 | 수용(표면화) | PLAN 설계·동일 구조 VO 추가는 Simplicity-First와 충돌. 표면화하여 기록 |
| `toView` 중복 / repo 직접 주입 / SecureRandom 중복 / Answer 수동생성 | 수용 | public·admin 의도적 디커플링·조기 공유추상화 회피(M3/M4 선례). ~6줄 |

## 결정 / 이탈 사항

- **PLAN 정정(컨벤션, 동작 무변경)**: 약어 금지(`surveyRepo`→`surveyRepository`, `s/r/ad/qs/opts/om` → 서술형), Jackson 3(테스트 `com.fasterxml.jackson`→`tools.jackson`, `.asText()`→`.asString()`), 테스트 메서드명 한국어 문장형, 뷰 조립에 `QuestionVo.from` 팩토리 사용.
- **PLAN 대비 보강(의도적)**: PLAN의 `validateMatchesType`는 SINGLE_CHOICE에서 `questionOptionId != null`만 검증 → 멤버십 검증 추가(코드리뷰 발견, FK 부재로 데이터 무결성 직결). PublicResponseService에 `QuestionOptionRepository` 주입. 음성 테스트 1종 추가.
- **REST Docs**: request body 배열의 optional 필드(`question_option_id`/`scale_value`)가 payload에 부재 시 타입 추론 불가 → `FieldTypeRequiredException` → `.type(JsonFieldType.NUMBER)` 명시. (M4는 optional이 present-null이라 추론됐던 차이.)
- ADR 트리거 없음.
