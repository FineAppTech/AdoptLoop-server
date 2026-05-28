# ADR-0009: Spring REST Docs 강제화 — JUnit Extension 방식

- 상태: Accepted
- 날짜: 2026-05-28
- 관련: M1 Foundation (Task 1.1 dependencies), 모든 컨트롤러 테스트, TECH_STACK

## Context
초기 설계 단계에서 API 문서는 `docs/AdoptLoop_API.yaml`(수기 OpenAPI)만 유지하기로 했다. 그러나 토이 일정에서도 다음 문제가 예상된다.
1. **표류**: 코드/테스트는 바뀌었는데 yaml만 안 고쳐져 문서·구현 어긋남이 누적.
2. **신뢰의 단일 출처 없음**: 리뷰어가 "이 PR이 API를 어떻게 바꾸는가"를 yaml diff로만 봐야 함 — 실제 동작과 일치한다는 보장 없음.

Spring REST Docs는 컨트롤러 테스트에서 실제 요청/응답을 캡처해 문서 스니펫을 생성한다. 즉 **테스트 통과 = 문서 최신**을 강제할 수 있다. 강제 방식은 세 가지 옵션이 있었다.
- A. 컨벤션만 (`andDo(document(...))` 호출 합의) — 까먹어도 빌드 통과.
- B. 스니펫 수 ↔ OpenAPI endpoint 수 CI 검증 — 중간 강제력, 매칭 로직 유지비.
- C. JUnit Extension으로 컨트롤러 테스트가 `document()` 호출 안 하면 fail — 강한 강제력.

## Decision
- **Spring REST Docs 도입**: `org.springframework.restdocs:spring-restdocs-mockmvc` 의존성 추가.
- **강제 방식: 옵션 C**. 컨트롤러 테스트 베이스 클래스에 JUnit 5 Extension 적용 — `document("operation-id", ...)` 호출이 한 번도 없으면 테스트 실패. (구현 디테일은 ADR 대상 아님 — Task 단위로 결정.)
- **출력 포맷: AsciiDoc**. REST Docs 기본 출력 + `org.asciidoctor.jvm.convert` Gradle 플러그인으로 `build/asciidoc/html5/index.html` 생성.
- **발행 위치**:
  - 로컬: `./gradlew build` 결과물을 `build/asciidoc/html5/`에서 직접 확인.
  - CI: GitHub Actions에서 `actions/upload-artifact@v4`로 PR마다 산출 HTML 업로드.
- **하지 않는 것**: Markdown 출력 변환, GitHub Pages 자동 발행, Confluence 동기화 — 7일 토이 일정 범위 밖.

## Consequences
- ✅ 모든 컨트롤러 endpoint는 자동 문서화됨 — 안 하면 빌드 깨짐.
- ✅ PR 리뷰어가 산출 HTML을 다운로드해 "이 PR의 API 변경"을 실 동작 기반으로 검증 가능.
- ✅ 자동 생성 docs가 단일 진실 소스 — 수기 `AdoptLoop_API.yaml`은 초기 설계 산출물로 박제(이후 동기화 부담 제거).
- ❌ 컨트롤러 테스트 작성 비용 증가: `document()` 호출 + field/parameter descriptors.
- ❌ AsciiDoc 산출물은 artifact 다운로드해야 봄 — Markdown 만큼 즉시성은 없음.
- 추후 다시 봐야 할 조건: 운영 단계 진입 또는 외부 API 공개 시 GitHub Pages 자동 발행 / OpenAPI 재생성을 검토 — 새 ADR로 supersede.
