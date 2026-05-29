# 작업 보고서 — M2 Task 2.2: 8개 Repository

- **Milestone / Task:** M2 Domain Entities + Adoption CRUD / Task 2.2
- **브랜치:** `feat/adoption-crud`
- **날짜:** 2026-05-29

## 변경 파일 목록

| 구분 | 파일 | 파생 쿼리 메서드 |
|------|------|------------------|
| 생성 | `domain/repository/AdoptionRepository.kt` | findAllByAdminId |
| 생성 | `domain/repository/SurveyRepository.kt` | findByPublicSlug, findAllByAdoptionId |
| 생성 | `domain/repository/QuestionRepository.kt` | findAllBySurveyIdOrderByOrderIndex, deleteAllBySurveyId |
| 생성 | `domain/repository/QuestionOptionRepository.kt` | findAllByQuestionIdInOrderByOrderIndex, deleteAllByQuestionIdIn |
| 생성 | `domain/repository/SurveyResponseRepository.kt` | findByAccessToken, countBySurveyIdAndStatus, countBySurveyId, findAllBySurveyIdAndStatus |
| 생성 | `domain/repository/AnswerRepository.kt` | deleteAllBySurveyResponseId, findAllBySurveyResponseId, findAllBySurveyResponseIdIn |
| 생성 | `domain/repository/AnalysisRepository.kt` | findFirstBySurveyIdOrderByCreatedAtDesc(Optional), findAllBySurveyIdOrderByCreatedAtDesc |
| 생성 | `domain/repository/ActionItemRepository.kt` | findAllByAdoptionId |

> `AdminRepository.kt`(M1 Task 1.4)는 변경 없음. PLAN은 한 블록으로 제시하나 "컨벤션 통일" 지침대로 인터페이스별 8개 파일로 분리.

## 검증 + 셀프 리뷰

**성격:** config성(Spring Data 인터페이스 선언, 구현 없음) → 인라인 셀프 리뷰. `/code-review` 미실행.

| 점검 | 결과 |
|------|------|
| 8개 파일 분리, package `domain.repository` | ✅ |
| 파생 쿼리 메서드명 ↔ 엔티티 프로퍼티 전수 대조 | ✅ 일치 |
| import 정합 (`ResponseStatus`, `Optional`) | ✅ |
| `./gradlew compileTestKotlin` | ✅ BUILD SUCCESSFUL |
| `./gradlew test` (전체) | ✅ BUILD SUCCESSFUL — M1 7개 회귀 0 |

**핵심 검증 — 쿼리 메서드 파싱:** Spring Data JPA는 컨텍스트 부팅 시 리포지토리 프록시를 생성하며 파생 쿼리 메서드명을 엔티티 프로퍼티에 대해 파싱·검증한다. 메서드명에 존재하지 않는 프로퍼티가 있으면 부팅 실패. 전체 `test` GREEN = 8개 리포지토리의 모든 메서드명이 유효함을 의미(컴파일만으로는 잡지 못하는 오류를 지금 확인).

## 결정 / 이탈 사항

- **파일 분리**: PLAN 한 블록 → 인터페이스별 8개 파일(PLAN 지침 "컨벤션 통일" 반영).
- 파생 `deleteAllBy...`는 Spring Data가 트랜잭션을 자동 부여 — 별도 `@Modifying`/`@Transactional` 불필요(엔티티 로드 후 삭제 방식). M3·M5·M6 호출처에서 사용.
- ADR 트리거 없음.
