# 작업 보고서 — M1 Task 1.4: Enums + Admin entity + Repository (TDD)

- **Milestone / Task:** M1 Foundation / Task 1.4
- **브랜치:** `feat/foundation`
- **날짜:** 2026-05-29

## 변경 파일 목록

| 구분 | 파일 | 핵심 변경 |
|------|------|-----------|
| 생성 | `domain/Enums.kt` | 7개 enum (Adoption/Survey/Question/Axis/Response/Priority/Todo status) |
| 생성 | `domain/Admin.kt` | `@Entity admins`, `@CreatedDate` 감사, IDENTITY PK |
| 생성 | `domain/repository/AdminRepository.kt` | `JpaRepository<Admin,Long>` + `findByKeyHash` |
| 생성 | `config/JpaConfig.kt` | `@EnableJpaAuditing` |
| 생성 | `test/IntegrationTestBase.kt` | Testcontainers postgres + `@ServiceConnection` + `@BeforeEach` TRUNCATE |
| 생성 | `test/domain/AdminRepositoryTest.kt` | save & findByKeyHash (+ createdAt 감사 검증) |
| 수정 | `build.gradle.kts` | **`spring-boot-flyway` 모듈 추가** (디버깅 결과, 아래 참조) |

## 검증 + 셀프 리뷰 + /code-review

**TDD 사이클:**
- RED: 테스트 실행 → `Unresolved reference 'Admin'/'AdminRepository'` 컴파일 실패 (예상).
- GREEN: 구현 후 → `BUILD SUCCESSFUL`, 테스트 PASS.

**Flyway 파이프라인 검증** (Task 1.3에서 이연한 부분 — 여기서 충족):
- `Creating Schema History table "flyway_schema_history"` → `Migrating schema "public" to version "1 - init"` → `Successfully applied 1 migration ... now at version v1`.
- JPA `ddl-auto: validate` 통과(엔티티↔admins 정합) + repository save/find 동작.

**디버깅 (GREEN 첫 시도 실패 → 근본원인 추적):**
- 증상: `SchemaManagementException: Schema-validation: missing table [admins]` — Flyway 로그 전무.
- 근본원인: **Spring Boot 4.0이 autoconfiguration을 기술별 모듈로 분리**. `flyway-core`만으로는 `FlywayAutoConfiguration`이 클래스패스에 없어 Flyway 미실행. `spring-boot-autoconfigure-4.0.6.jar`에 해당 클래스 부재 확인.
- 수정: `implementation("org.springframework.boot:spring-boot-flyway")` 추가 → Flyway 정상 실행, GREEN.

**`/code-review` (medium, 로직 작업 자동 실행) — finder 3 + 자체검증:**
| 발견 | 처리 |
|------|------|
| 테스트가 `createdAt`(감사 결과) 미검증 → 감사 깨져도 green | ✅ 적용 — `assertTrue(saved.createdAt.isAfter(Instant.EPOCH))` 추가, 재실행 PASS |
| `createdAt=Instant.EPOCH`가 감사 실패 시 NOT NULL 가림 | △ 미변경 — 위 어서션이 감사 회귀를 잡음. EPOCH는 PLAN 명시 관용패턴 |
| `Enums.kt` 7개 enum 미사용(투기적) | ✗ 미변경 — PLAN이 의도적으로 enum 일괄 정의(M2+ 기반) |

## 결정 / 이탈 사항

- **PLAN 갭 발견 + 수정**: PLAN Task 1.1 의존성에 `spring-boot-flyway` 누락 → Spring Boot 4.0에선 Flyway 동작 불가. PLAN Task 1.1 deps 정정 완료. 의존성 추가는 (사용자 결정) **본 Task 1.4 커밋에 포함**(1.1 커밋 고정 유지).
- **테스트 이탈(개선)**: PLAN의 `AdminRepositoryTest`는 id·name만 검증하나, 이 Task가 도입하는 감사 기능을 검증하려 `createdAt` 어서션 1줄 추가. (/code-review 권고)
- ADR 트리거 없음 (의존성/구현 디테일).
