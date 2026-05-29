# 작업 보고서 — M1 Task 1.6: AdminBootstrap (env 기반 시드)

- **Milestone / Task:** M1 Foundation / Task 1.6
- **브랜치:** `feat/foundation`
- **날짜:** 2026-05-29

## 변경 파일 목록

| 구분 | 파일 | 핵심 변경 |
|------|------|-----------|
| 생성 | `admin/AdminBootstrap.kt` | `CommandLineRunner` — env `ADOPTLOOP_ADMIN_NAME/KEY` 있으면 SHA-256 해시로 admin 시드(멱등), 없으면 no-op(prod+0건이면 warn) |
| 생성 | `test/admin/AdminBootstrapTest.kt` | 3 케이스 (시드 / 멱등 / blank no-op) |
| 수정 | `test/IntegrationTestBase.kt` | **싱글톤 컨테이너 패턴**으로 변경 (아래 디버깅) |
| 수정 | `docs/AdoptLoop_PLAN.md` | Task 1.4 `IntegrationTestBase` 블록도 싱글톤 패턴으로 정정 |

## 검증 + 셀프 리뷰 + /code-review

**TDD:** RED(`AdminBootstrap` 미정의 컴파일 실패) → 구현 → GREEN.

**전체 스위트:** 4 클래스 / 7 테스트, **0 실패·에러·스킵** (`BUILD SUCCESSFUL`, 5s).

**디버깅 (GREEN 첫 시도에서 AdminRepositoryTest 회귀):**
- 증상: `HikariPool-2 ... Connection to localhost:54946 refused` — `AdminRepositoryTest`가 30초 타임아웃 실패(이전엔 통과).
- 근본원인: `@Testcontainers`/`@Container`는 컨테이너를 **테스트 클래스마다 start/stop**. `AdminBootstrapTest`·`AdminRepositoryTest`가 동일 컨텍스트 설정이라 Spring이 컨텍스트를 **캐싱·재사용**하는데, 앞 클래스 종료 시 stop된 컨테이너 포트를 캐시 컨텍스트의 Hikari 풀이 계속 참조 → 연결 거부. (테스트 2개일 땐 미발현, 같은 설정 3번째 클래스 추가로 드러남.)
- 수정: **싱글톤 컨테이너 패턴** — `@Testcontainers`/`@Container` 제거, 정적 필드를 `.also { it.start() }`로 수동 1회 기동(JVM 내내 유지, Ryuk가 종료 시 정리). `@ServiceConnection`은 Boot의 `ServiceConnectionContextCustomizerFactory`가 정적 필드를 스캔하므로 `@Testcontainers` 없이도 동작.

**`/code-review` (medium, 로직):**
| 발견 | 처리 |
|------|------|
| IntegrationTestBase 싱글톤 변경 정합성 (리뷰어 별도) | ✅ 이상 없음 — ServiceConnection 무(無)@Testcontainers 동작·Ryuk 정리·init 1회 안전 확인 |
| `blank key` 테스트의 `assertNull(...)` 중복(보호 0) | ✅ 적용 — 제거 + 미사용 import 정리 |
| bootstrap save 경로 `createdAt` 감사 미검증 | ✗ 미적용 — AdminRepositoryTest가 동일 save 경로 감사를 이미 검증(Task 1.4), 중복 |

프로덕션 코드 확인: 해시가 AdminKeyFilter와 byte-동일(Kotlin UTF-8 `toByteArray` + `%02x`) → 시드한 키로 인증 통과. 멱등성(findByKeyHash null-check, 단일스레드 startup) 정상.

## 결정 / 이탈 사항

- **검증 방법 이탈**: PLAN Step 2(수동 `bootRun` + `psql`) 대신 **TDD 통합 테스트**로 검증 — `bootRun`은 Bedrock 오토컨피그 AWS region 미설정으로 기동 실패 가능 + 자동 테스트가 회귀 보호.
- **테스트 인프라 수정**: `IntegrationTestBase` 싱글톤 컨테이너 패턴(공유 인프라 버그픽스). PLAN Task 1.4 블록도 동일 정정. Task 1.6 커밋에 포함.
- **네이밍**: `repo`→`adminRepository`, `env`→`environment` (약어 금지).
- ADR 트리거 없음.
- 선재 관찰(범위 외): `AdoptloopServerApplicationTests`는 `IntegrationTestBase` 미상속·default(local) 프로파일이라 로컬 compose DB 의존 — CI에서 DB 없으면 깨질 수 있음. 후속 정리 대상.
