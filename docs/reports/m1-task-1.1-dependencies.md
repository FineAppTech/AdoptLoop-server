# 작업 보고서 — M1 Task 1.1: 의존성 추가

- **Milestone / Task:** M1 Foundation / Task 1.1
- **브랜치:** `feat/foundation`
- **날짜:** 2026-05-29

## 변경 파일 목록

| 구분 | 파일 | 핵심 변경 |
|------|------|-----------|
| 수정 | `build.gradle.kts` | PLAN Task 1.1 내용으로 전체 교체 |

주요 변경 내용:
- **플러그인 추가**: `kotlin("plugin.jpa") 2.2.21`, `org.asciidoctor.jvm.convert 4.0.4`
- **Spring AI BOM** `1.0.4` import (`dependencyManagement`)
- **구현 deps**: data-jpa, validation, thymeleaf, flyway-core, flyway-database-postgresql, spring-ai-starter-model-bedrock-converse
- **런타임 deps**: postgresql, spring-boot-docker-compose(developmentOnly)
- **테스트 deps**: spring-restdocs-mockmvc, spring-boot-testcontainers, testcontainers(postgresql/junit-jupiter), mockk 1.13.13, springmockk 4.0.2
- **REST Docs 빌드 연결**: `generated-snippets` 디렉토리를 test 출력에 등록, `asciidoctor` 태스크가 snippets 입력 + test 의존

## 검증 결과

| Step | 명령 | Expected | 실제 |
|------|------|----------|------|
| 2 | `./gradlew dependencies --configuration testRuntimeClasspath` | EXIT 0, bedrock-converse + restdocs-mockmvc 포함 | ✅ EXIT=0, `spring-ai-starter-model-bedrock-converse → 1.0.4`, `spring-restdocs-mockmvc → 4.0.0` 확인 |
| 3 | `./gradlew tasks --group documentation` | `asciidoctor` 태스크 등록 | ✅ `asciidoctor - Generic task to convert AsciiDoc files...` 출력 |

## 결정 / 이탈 사항

- PLAN의 Step 1 코드 블록을 **그대로** 적용 — 이탈 없음.
- ADR 트리거 없음 (의존성 추가는 일반 구현 디테일).
- `spring-restdocs-mockmvc`는 BOM에 의해 `4.0.0`으로 해석됨 (PLAN은 버전 미지정 → 정상).

## Testcontainers 2.0 좌표 주의

- Spring Boot 4.0.6은 BOM으로 Testcontainers **2.0.5**를 강제하며, 2.0부터 모듈 아티팩트가 `testcontainers-` 접두사로 리네임됨. 따라서 좌표는 `testcontainers-postgresql` / `testcontainers-junit-jupiter`를 사용 (구 `postgresql` / `junit-jupiter`는 미해결). PLAN Task 1.1도 동일 정정.
- **검증 한계 메모**: `./gradlew dependencies`는 미해결 의존성이 있어도 EXIT 0으로 종료한다(false positive). 의존성 해석은 `compileTestKotlin` 등 컴파일/테스트로 확인해야 확실하다.
