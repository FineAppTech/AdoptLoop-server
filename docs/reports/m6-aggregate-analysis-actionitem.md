# 작업 보고서 — M6: 집계 + AI 분석 + 액션 아이템

- **Milestone:** M6 (Task 6.1 집계 / 6.2 AI 분석 / 6.3 액션 아이템 / 6.4 컨트롤러 테스트)
- **브랜치:** `feat/analysis` (base: `c2efca5` — PR #9/M5 머지본)
- **날짜:** 2026-05-29
- **성격:** 로직 → `/code-review`(medium, finder 3종 병렬 + triage) 수행

## 개요

제출된 응답을 집계(SELECT)하고, 집계+도입 정보를 Bedrock(Claude Haiku)에 보내 정착도 점수·신호·제안 액션을 받아 저장하며, 제안을 액션 아이템으로 채택·상태관리하는 관리자 플로우. 신규 컨트롤러 2종(`AnalysisController`/`ActionItemController`)은 ADR-0009에 따라 `ControllerTestBase` + `documentApi` 적용.

| 메서드 | 경로 | 동작 | 음성 |
|--------|------|------|------|
| GET | `/api/admin/surveys/{surveyId}/aggregate` | 집계 조회 | 미소유→403 |
| POST | `/api/admin/surveys/{surveyId}/analyses` | AI 분석 실행(201) | 미소유→403, 마감 전→403, LLM실패→503 |
| GET | `/api/admin/surveys/{surveyId}/analyses/latest` | 최신 분석 조회 | 미소유→403, 없음→404 |
| GET | `/api/admin/adoptions/{adoptionId}/action-items` | 액션 목록 | 미소유→403 |
| POST | `/api/admin/adoptions/{adoptionId}/action-items` | 채택(201) | 미소유→403, 타 도입 분석→400 |
| PATCH | `/api/admin/action-items/{id}` | 상태 변경 | 미소유→403 |

## 변경 파일 목록 (13파일 신규)

| 구분 | 파일 | 핵심 |
|------|------|------|
| 생성 | `analysis/AggregateDtos.kt` | `AggregateRes` + sealed `QuestionAggregateVo`(Choice/Scale/Text) + `DistributionBucketVo` |
| 생성 | `analysis/AggregateService.kt` | `@Transactional(readOnly)`. 제출응답 수·참여율·문항별 집계(분포/평균/텍스트). **소유권 검증**(리뷰 반영) |
| 생성 | `analysis/AnalysisPrompt.kt` | 분석 프롬프트(집계 JSON 직렬화 포함) |
| 생성 | `analysis/AnalysisParser.kt` | LLM JSON → `ParsedAnalysisDto`. `.values()` 기반 파싱(member `map` 회피), title 가드 |
| 생성 | `analysis/AnalysisDtos.kt` | `AnalysisRes`/`SuggestedActionItemVo`/`AnalysisRunRes` |
| 생성 | `analysis/AnalysisService.kt` | LLM 호출은 tx 밖(M4 선례), 단일 save 원자 저장. `run`/`latest` 소유권·마감 검증 |
| 생성 | `analysis/AnalysisController.kt` | aggregate/run/latest 3 endpoint |
| 생성 | `actionitem/ActionItemDtos.kt` | `ActionItemCreateReq`/`UpdateReq`/`Res(+from)` |
| 생성 | `actionitem/ActionItemService.kt` | `@Transactional`. 채택(타 도입 분석 400)·목록·상태변경, 전부 소유권 검증 |
| 생성 | `actionitem/ActionItemController.kt` | list/adopt/update 3 endpoint |
| 생성 | `test/analysis/AggregateServiceTest.kt` | SCALE 평균·참여율 집계 |
| 생성 | `test/analysis/AnalysisControllerTest.kt` | aggregate happy(200) + 미소유 403 + `documentApi`×2 |
| 생성 | `test/actionitem/ActionItemControllerTest.kt` | adopt happy(201) + `documentApi`×1 |

## 검증 (GREEN)

1. **전체 스위트**: BUILD SUCCESSFUL — **28 tests / 0 fail / 0 err** (M1~M5 24 + M6 4). 회귀 0.
2. **REST Docs 스니펫 3종**: `get-survey-aggregate`, `get-survey-aggregate-forbidden`(403), `adopt-action-items`(요청/응답 필드). `documentApi` 강제(ADR-0009) 충족.

### 테스트 케이스

| 테스트 | 검증 |
|------|------|
| `SCALE 평균과 참여율을 집계한다` (서비스) | 응답 3건[3,4,5]/대상 10 → participants=3, rate=0.3, average=4.0 |
| `aggregate는 참여율과 SCALE 문항 집계를 반환한다` | GET 200, participants=3, SCALE subtype 필드 문서화 |
| `남의 설문 집계를 조회하면 403` | 타 관리자 설문 GET → 403 (IDOR 차단, 리뷰 발견 반영) |
| `analysis로부터 액션 아이템을 채택한다` | POST 201, status=TODO, 요청/응답 전 필드 문서화 |

## 사전 검증으로 확인한 사실 (PLAN 정정)

- **의존 API 전부 실재**: `SurveyResponseRepository.countBySurveyIdAndStatus`/`findAllBySurveyIdAndStatus`, `AnswerRepository.findAllBySurveyResponseIdIn(Collection<Long>)`, `AnalysisRepository.findFirstBySurveyIdOrderByCreatedAtDesc`(Optional), `ActionItemRepository.findAllByAdoptionId` — 시그니처 일치. 엔티티 생성자·enum(Priority/TodoStatus/Axis/ResponseStatus)도 일치.
- **예외 매핑**: `NoSuchElementException`→404, `ResponseStatusException`→상태 유지, `LlmTransientException`→503 (GlobalExceptionHandler). PLAN의 `NoSuchElementException("survey")`는 404로 정상 매핑.
- **인증/소유권**: `AdminContext.require(): Long`(adminId), `AdminKeyFilter`가 `/api/admin/**`에서 X-Admin-Key→adminId 설정. 소유권 검증은 서비스에서 `adoption.adminId != adminId → 403`(M3~M5 일관 패턴).
- **Bedrock**: ChatClient는 실 빈(autoconfig, region 기본 us-east-1) — aggregate/adopt 테스트 경로는 LLM 미호출이라 부팅만으로 충분.

## /code-review (medium) — finder 3종(정확성/인가/일관성) + triage

| finder 지적 | 판정 | 조치 |
|------|------|------|
| **`GET /aggregate` 인가 누락 (IDOR)** | **수정** | `adminContext.require()`만 호출·소유권 미검증 → 관리자가 타인 설문의 응답 분포·주관식 답변 열람. 같은 컨트롤러 `run`/`latest`·전 action-item은 검증함. `AggregateService.aggregate(adminId, surveyId)`로 소유권 검증 추가(이미 adoption 로드 → 추가 쿼리 없음) + 음성 테스트 |
| **AnalysisParser `title` null 미가드** | **수정(경미)** | `description`은 가드하나 `title` 미가드 → LLM이 title 누락 시 빈 문자열 제안 생성. M4 SurveyDraftParser idiom(`?.asString()?.takeIf(isNotBlank) ?: error()`)으로 일관화(→ 503) |
| action-item adopt 교차 도입 가능성 | 기각 | adoption 소유권 + `survey.adoptionId != adoptionId`(400)로 이미 차단(finder도 인정). 잔여 enumeration은 토이 비민감 |
| PATCH 403-vs-404 enumeration | 수용 | 전 코드베이스 공통(found-not-owned=403, missing=404). M6 단독 변경 부적절 |
| 소유권 검증 중복(~6회)·dirty-check·survey 보조 로드 | 수용 | M3/M4/M5 조기추상화 회피·PublicResponseService 선례 |

## 결정 / 이탈 사항

- **의도적 이탈 — LLM tx 분리**: PLAN은 `AnalysisService`를 클래스 `@Transactional`로 두고 그 안에서 LLM 호출. M4에서 확립한 전역 원칙(느린 외부호출이 DB 트랜잭션/커넥션을 잡지 않게)에 따라 `run()`은 비트랜잭션 → LLM은 tx 밖, 단일 `save()`의 암묵 tx로 원자 저장. 엔티티에 lazy 연관이 없어(FK 전부 plain Long) 비tx 읽기 안전. `latest()`는 읽기전용 tx 유지.
- **Jackson 3 핵심 발견 — `JsonNode`에 member `<R> R map(Function)`**: Boot4의 `tools.jackson.databind.JsonNode`는 member `map`을 가져 `node.map{}`이 Iterable 확장이 아닌 이쪽으로 묶임(람다 결과를 그대로 반환) → 타입 추론 깨짐. **`.values()`(→`Collection<JsonNode>`)로 받아 stdlib `map` 사용**. (M4는 `mapIndexed`만 써 우회됐던 차이.) → 메모리 기록 필요.
- **PLAN 정정(컨벤션·Boot4)**: `com.fasterxml.jackson.*`→`tools.jackson.*`, `.asText()`→`.asString()`(숫자 `.asInt()`는 3.1.2에 실재 유지), 약어 금지(`surveyRepo`→`surveyRepository` 등 서술형), 명시적 import, 테스트명 한국어 문장형.
- **PLAN 대비 보강(리뷰)**: aggregate 소유권 검증 + 음성 테스트, parser title 가드.
- ADR 트리거 없음(구현 디테일·정정 범위).
