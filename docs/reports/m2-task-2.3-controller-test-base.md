# 작업 보고서 — M2 Task 2.3: ControllerTestBase + REST Docs 강제 인프라

- **Milestone / Task:** M2 Domain Entities + Adoption CRUD / Task 2.3
- **브랜치:** `feat/adoption-crud`
- **날짜:** 2026-05-29
- **관련 ADR:** [ADR-0009](../adr/0009-spring-restdocs-enforcement.md)

## 변경 파일 목록

| 구분 | 파일 | 핵심 |
|------|------|------|
| 생성 | `test/.../restdocs/DocCallTracker.kt` | ThreadLocal 호출 추적 (reset/mark/wasCalled) |
| 생성 | `test/.../restdocs/RequireDocumentationExtension.kt` | JUnit Extension — afterEach에서 `documentApi` 미호출 시 `AssertionError` |
| 생성 | `test/.../restdocs/RestDocs.kt` | `documentApi(identifier, *snippets)` 헬퍼 — mark 후 `MockMvcRestDocumentation.document` 위임 |
| 생성 | `test/.../ControllerTestBase.kt` | IntegrationTestBase 상속 + 2개 Extension + MockMvc documentationConfiguration |
| 생성 | `src/docs/asciidoc/index.adoc` | AsciiDoc 합본 placeholder |

## 검증 + 셀프 리뷰

**성격:** config성(테스트 인프라, 비즈니스 로직 없음) → 인라인 셀프 리뷰. `/code-review` 미실행.

| 점검 | 결과 |
|------|------|
| `RequireDocumentationExtension` 가드 로직 | ✅ `executionException.isEmpty && !wasCalled`일 때만 fail — 테스트 본문이 이미 실패한 경우 그 실패를 우선 신호로 두고 문서화 강제는 미적용(정확) |
| `documentApi` 우회 불가 | ✅ 표준 `document(...)` 대신 호출 강제, 내부에서 `DocCallTracker.mark()` |
| ThreadLocal 적합성 | ✅ JUnit 기본 순차 실행, 테스트별 beforeEach reset |
| `ControllerTestBase` 상속 체인 | ✅ IntegrationTestBase(컨테이너+truncate) + RestDocumentationExtension + RequireDocumentationExtension |
| import 정합 (Boot 4.0 패키지) | ✅ |
| `./gradlew compileTestKotlin` | ✅ BUILD SUCCESSFUL |
| `./gradlew test` (전체, 회귀) | ✅ BUILD SUCCESSFUL — M1 7개 회귀 0 |

**강제 동작 검증 위치:** 본 태스크 자체에는 자동 테스트가 없음(PLAN 설계). 강제력은 다음 Task 2.4 `AdoptionControllerTest`에서 자연 검증된다 — `documentApi` 호출 시 그린, 미호출 시 Extension이 fail.

## 결정 / 이탈 사항

- **네이밍 판단**: 테스트 인프라의 `mvc`(MockMvc)·`ctx`(ExtensionContext)는 Spring REST Docs/JUnit 표준 idiom이자 PLAN(M2~M6 컨트롤러 테스트 스니펫)에서 일관 사용되는 식별자라 그대로 유지. 약어 금지 컨벤션([[naming-convention-no-abbreviation]])은 도메인/영속 식별자(`objectMapper`/`adminRepository`/`adoptionRepository`, Task 2.4)에 적용. 이견 시 `mvc`→`mockMvc` 전체 치환 가능.
- **검증 범위**: PLAN Step 6은 `compileTestKotlin`만 요구. 추가로 전체 `test` 실행해 M1 회귀 확인.
- ADR 트리거 없음 (ADR-0009 구현체).
