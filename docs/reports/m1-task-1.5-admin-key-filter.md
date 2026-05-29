# 작업 보고서 — M1 Task 1.5: AdminKeyFilter + SecurityConfig

- **Milestone / Task:** M1 Foundation / Task 1.5
- **브랜치:** `feat/foundation`
- **날짜:** 2026-05-29

## 변경 파일 목록

| 구분 | 파일 | 핵심 변경 |
|------|------|-----------|
| 생성 | `admin/auth/AdminContext.kt` | `@Component @RequestScope`, `adminId` + `require()` |
| 생성 | `admin/auth/AdminKeyFilter.kt` | `OncePerRequestFilter`, `X-Admin-Key`→SHA-256→`findByKeyHash`, `/api/admin` 외 skip |
| 생성 | `test/admin/AdminKeyFilterTest.kt` | 401 두 케이스 (missing/invalid key) |
| 수정 | `docs/AdoptLoop_PLAN.md` | `@AutoConfigureMockMvc` Boot 4.0 패키지 정정(3곳) + Task 1.5 공허 단언 2줄 정정 |

**SecurityConfig.kt: 생성 보류** — PLAN Step 5 명시(`@Component OncePerRequestFilter` 자동 등록으로 충분, Spring Security 미사용). Files 목록엔 있으나 Step 5 지시 따름.

## 검증 + 셀프 리뷰 + /code-review

**TDD:**
- RED: 수정판 단언으로 실행 → `Status expected:<401> but was:<404>` (필터 부재 → 404). 정상 RED.
- GREEN: 필터 구현 후 → 전체 스위트 `BUILD SUCCESSFUL`.

**`/code-review` (medium, 보안 민감 → 정확성·보안 / Spring 와이어링 / 정리) — finder 결과:**

| 발견 | 처리 |
|------|------|
| `requestURI`가 context-path 포함 → context-path 설정 시 필터 우회(인증 무력화) | ✅ **적용**. (1차 `servletPath` 시도 → MockMvc에서 빈 값이라 테스트 깨짐 → `requestURI.removePrefix(contextPath)`로 교정) |
| prefix 경계 없음(`/api/administration` 과잉 게이팅) | △ 미적용 — 현재 그런 경로 없음, 후속 판단거리 |
| `response.status=401`만 → ERROR 디스패치 덮을 우려 | △ 미적용 — short-circuit이라 디스패처 미도달, 동작·테스트 정상 |
| `raw.toByteArray()` 플랫폼 charset 우려 | ❌ 오판 — **Kotlin `toByteArray()` 기본 UTF-8**(Java와 다름). Task 1.6 시드도 Kotlin이라 일관 |
| `admin.id` Long? | △ 미적용 — DB fetch 후 non-null, 저심각 |
| 와이어링(@RequestScope→싱글톤 프록시·자동등록·스레드안전) | ✅ 정상(별도 finder 0건) |

## 결정 / 이탈 사항

- **PLAN 테스트 결함 수정**: PLAN의 `andExpect { it.response.status == 401 }`는 람다가 `ResultMatcher`로 변환되며 boolean을 계산만 하고 버려 **아무것도 단언하지 않음**(필터가 깨져도 green). → `andExpect(status().isUnauthorized())`로 교정. 미사용 import 제거.
- **Boot 4.0 패키지 이동**: `@AutoConfigureMockMvc`가 `org.springframework.boot.test.autoconfigure.web.servlet` → `org.springframework.boot.webmvc.test.autoconfigure`(spring-boot-webmvc-test jar). PLAN 3곳 정정.
- **네이밍 컨벤션**: `adminRepo` → `adminRepository`(약어 금지).
- **보안 하드닝**: `shouldNotFilter` 경로 매칭을 context-path 무관하게(`requestURI.removePrefix(contextPath)`).
- ADR 트리거 없음.
