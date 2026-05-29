# 작업 보고서 — M1 Task 1.2: compose.yaml + application 프로파일

- **Milestone / Task:** M1 Foundation / Task 1.2
- **브랜치:** `feat/foundation`
- **날짜:** 2026-05-29

## 변경 파일 목록

| 구분 | 파일 | 핵심 변경 |
|------|------|-----------|
| 생성 | `compose.yaml` | postgres 16-alpine, DB/USER/PW=`adoptloop`, 5432 노출, `pg_isready` healthcheck |
| 수정 | `src/main/resources/application.yaml` | 공통 설정 추가 (default profile=local, Jackson SNAKE_CASE, JPA validate/open-in-view=false/format_sql, Flyway, `adoptloop.*` 커스텀 프로퍼티) |
| 생성 | `src/main/resources/application-local.yaml` | 로컬 postgres datasource |
| 생성 | `src/main/resources/application-test.yaml` | flyway clean-disabled=false, hibernate SQL 로그 WARN |
| 생성 | `src/main/resources/application-prod.yaml` | env(`DB_URL`/`DB_USER`/`DB_PASSWORD`) 기반 datasource |
| 생성 | `src/main/kotlin/com/tnear/adoptloop/config/SlackProperties.kt` | `@ConfigurationProperties("adoptloop.slack")` data class |
| 수정 | `src/main/kotlin/com/tnear/adoptloop/AdoptloopServerApplication.kt` | `@ConfigurationPropertiesScan` 추가 |

## 검증 + 셀프 리뷰

| 명령 | Expected | 실제 |
|------|----------|------|
| `./gradlew compileKotlin` | 컴파일 성공 (config 클래스 + 어노테이션 변경) | ✅ `BUILD SUCCESSFUL` |
| `./gradlew compileTestKotlin` | 테스트 컴파일 성공 | ⚠️→✅ 최초 FAILED (testcontainers 미해결, Task 1.1 좌표 버그) → 좌표 수정 후 `BUILD SUCCESSFUL` |

셀프 리뷰 체크:
- 정확성: profile 분리(local/test/prod) + 공통 `application.yaml` 일관, `adoptloop.*` 커스텀 프로퍼티 ↔ `SlackProperties` 바인딩 경로 일치(`@ConfigurationPropertiesScan`).
- 단순성/잔여물: 추가 코드 없음, 미사용 import 없음.
- **교차 발견(중요)**: 테스트 컴파일에서 Task 1.1 의존성 좌표 버그 발견 → 별도 수정 (아래 결정/이탈 참조).

> Flyway `validate` / DB 연결은 V1 마이그레이션(Task 1.3) 이후 앱 기동 시 검증 가능.

## 결정 / 이탈 사항

- `AdoptloopServerApplication.kt`에서 PLAN은 어노테이션을 FQCN(`@org.springframework.boot.context.properties.ConfigurationPropertiesScan`)으로 표기했으나, 기존 파일이 `SpringBootApplication`을 import 단축형으로 쓰는 스타일과 일치시키기 위해 **import + 단축형**으로 작성. 동작 동일.
- 그 외 Task 1.2 PLAN 코드 블록 그대로 적용 — 이탈 없음.
- **Task 1.1 버그 발견(셀프 리뷰)**: `build.gradle.kts`의 testcontainers 좌표가 2.0에서 미해결 → `testcontainers-postgresql`/`testcontainers-junit-jupiter`로 수정. 태스크별 단일 커밋 원칙에 따라 **Task 1.1 커밋에 amend로 편입**(PLAN Task 1.1 + 1.1 보고서 포함). 본 커밋(1.2)에는 미포함.
- ADR 트리거 없음.
