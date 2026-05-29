# 작업 보고서 — M2 Task 2.4: Adoption DTO/Service/Controller (TDD)

- **Milestone / Task:** M2 Domain Entities + Adoption CRUD / Task 2.4
- **브랜치:** `feat/adoption-crud`
- **날짜:** 2026-05-29
- **성격:** 로직 → `/code-review`(medium) 실행

## 변경 파일 목록

| 구분 | 파일 | 핵심 |
|------|------|------|
| 생성 | `adoption/AdoptionDtos.kt` | CreateReq(@NotBlank/@Min) · UpdateReq(부분수정) · Res(+`from`) |
| 생성 | `adoption/AdoptionService.kt` | create/listForAdmin/get/update, 소유자 검증(FORBIDDEN), @Transactional |
| 생성 | `adoption/AdoptionController.kt` | `/api/admin/adoptions` GET·POST(201)·GET/{id}·PATCH/{id} |
| 생성 | `config/GlobalExceptionHandler.kt` | NSE→404, MethodArgumentNotValid→400, LlmTransient→503 |
| 생성 | `config/LlmTransientException.kt` | M4·M6 LLM 공통 예외 |
| 생성 | `test/adoption/AdoptionControllerTest.kt` | POST 생성 happy path + `documentApi` (ADR-0009) |
| 수정 | `test/ControllerTestBase.kt` | **AdminKeyFilter 명시 등록**(아래 디버깅) |

## 검증 (TDD RED→GREEN)

1. **RED**: 테스트 작성·실행 → 실패. 원인이 엔드포인트가 아니라 `com.fasterxml.jackson...ObjectMapper` **빈 없음**.
2. **Boot 4.0 발견 ①(Jackson 3)**: 이 프로젝트는 `tools.jackson.module:jackson-module-kotlin`(Jackson 3.1.2) 사용 + Spring Boot 4.0은 **Jackson 3 기본**. 빈 타입은 `tools.jackson.databind.ObjectMapper`. PLAN의 `com.fasterxml...` import 정정. (클래스패스 jar 검증으로 확정)
3. 프로덕션 코드 작성 후 재실행 → **여전히 실패**: `IllegalStateException: admin not authenticated`.
4. **Boot 4.0 발견 ②(필터 미등록)**: `ControllerTestBase`의 `webAppContextSetup` MockMvc는 `@AutoConfigureMockMvc`와 달리 **서블릿 필터 빈을 자동 등록하지 않음** → `AdminKeyFilter` 미실행 → `adminContext.adminId` null. `.addFilters(adminKeyFilter)` 추가로 해결. (M1 `AdminKeyFilterTest`는 `@AutoConfigureMockMvc`라 영향 없었음 — Task 2.4가 ControllerTestBase의 첫 소비자라 갭이 드러남.)
5. **GREEN**: `AdoptionControllerTest` 통과 + 전체 스위트 BUILD SUCCESSFUL(M1 회귀 0). REST Docs 스니펫 `build/generated-snippets/create-adoption/` 8종 생성, `documentApi` 추적(강제) 정상.

## /code-review (medium, 3 finder 병렬 + triage)

| finder 주장 | 판정 | 근거 |
|------|------|------|
| Jackson tools vs fasterxml 직렬화 불일치 | REFUTED | Boot 4.0은 tools.jackson 기본. 빈 주입·한글 응답 GREEN으로 입증 |
| @RequestScope ↔ webAppContextSetup 비호환/누수 | REFUTED | perform()마다 request scope 재생성. 필터→컨텍스트→컨트롤러 GREEN |
| update() explicit save() 누락 | REFUTED | @Transactional dirty-checking 관용 |
| `a.id!!` NPE | REFUTED | save 후 IDENTITY id 항상 존재 |
| update() @Transactional 미선언 | REFUTED | 클래스 레벨 @Transactional 적용 |
| BeforeEach truncate 순서 | REFUTED | admin 시드는 테스트 본문에서 perform 전 수행 |
| RequireDocExt abstract 오발 | REFUTED | 추상 클래스는 JUnit 미실행 |
| **`a`→`adoption` 약어** | **적용** | 약어 금지 규칙([[naming-convention-no-abbreviation]]) 정합 |
| update 입력 검증 부재 → DB 500 | **수정 완료** | 사용자 요청으로 수정 (아래) |
| ResponseStatusException 바디(ProblemDetail≠ErrorRes) | **수정 완료** | 사용자 요청으로 수정 (아래) |

### 추가 수정 (사용자 요청 — 보류 2건 반영, TDD)

| 수정 | 변경 | 검증 |
|------|------|------|
| update 입력 검증 | `AdoptionUpdateReq.targetCount`에 `@field:Min(1)`(nullable→present일 때만 검증, 부분수정 유지) + 컨트롤러 `update`에 `@Valid` | `target_count=0` PATCH → **400 VALIDATION_FAILED** (테스트 `target_count가 1 미만이면 400`) |
| 에러 바디 통일 | `GlobalExceptionHandler`에 `@ExceptionHandler(ResponseStatusException)` 추가 — status 유지, 바디를 `ErrorRes(code=status명, message=reason)`로 통일 | 타 admin 조회 → **403 code=FORBIDDEN** (테스트 `남의 도입을 조회하면 403`) |

- 새 REST Docs 스니펫 2종 생성: `update-adoption-invalid`, `get-adoption-forbidden` (에러 응답도 문서화).
- 두 동작 모두 통과 테스트로 직접 입증 → 인라인 검증으로 마침(전체 `/code-review` 재실행 생략).

## 결정 / 이탈 사항

- **PLAN 정정 ①**: 컨트롤러 테스트 `ObjectMapper` import `com.fasterxml.jackson` → `tools.jackson.databind`(Boot 4.0/Jackson 3). M3·M5·M6 컨트롤러 테스트 스니펫에도 동일 적용 필요 → PLAN 동기화.
- **인프라 수정**: `ControllerTestBase`에 `AdminKeyFilter` 등록(Task 2.3 산출물의 갭, Task 2.4 TDD가 드러냄 → 본 커밋 포함). PLAN Task 2.3 블록도 정정.
- **네이밍**: `repo`→`adoptionRepository`, `om`→`objectMapper`, `adminRepo`→`adminRepository`, `a`→`adoption`. 테스트 인프라 idiom `mvc`는 유지.
- **테스트 메서드명 한국어화(신규 컨벤션)**: 사용자 결정으로 M2부터 테스트 메서드명을 한국어 문장형으로 작성(`도입을 생성한다` 등). 단 `documentApi(...)` 식별자는 ASCII kebab 유지(스니펫 폴더명). M1 테스트(머지됨)는 현행 유지. WORKFLOW.md·메모리 반영.
- **보류 2건 → 수정 완료**: 사용자 요청으로 둘 다 수정 + TDD 케이스 추가(위 표). 더는 후속 항목 아님.
- ADR 트리거 없음.
