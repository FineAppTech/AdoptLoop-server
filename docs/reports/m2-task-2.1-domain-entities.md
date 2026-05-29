# 작업 보고서 — M2 Task 2.1: 8개 도메인 엔티티

- **Milestone / Task:** M2 Domain Entities + Adoption CRUD / Task 2.1
- **브랜치:** `feat/adoption-crud`
- **날짜:** 2026-05-29

## 변경 파일 목록

| 구분 | 파일 | 핵심 |
|------|------|------|
| 생성 | `domain/Adoption.kt` | admin_id·name·goal·target_audience·concern?·target_count·status, 감사 2개 |
| 생성 | `domain/Survey.kt` | adoption_id·title·public_slug(unique)·deadline(NOT NULL)·status·published_at?, 감사 2개 |
| 생성 | `domain/Question.kt` | survey_id·type·text·order_index·required·axis? (감사 없음) |
| 생성 | `domain/QuestionOption.kt` | question_id·text·order_index (감사 없음) |
| 생성 | `domain/SurveyResponse.kt` | survey_id·access_token(unique)·status·submitted_at?, 감사 2개 |
| 생성 | `domain/Answer.kt` | survey_response_id·question_id·text_value?·question_option_id?·scale_value? (감사 없음) |
| 생성 | `domain/Analysis.kt` | survey_id·4개 점수·JSONB 3개·raw_output, `created_at`만 |
| 생성 | `domain/ActionItem.kt` | adoption_id·analysis_id·title·description?·priority·status, 감사 2개 |

> Admin 엔티티는 M1 Task 1.4에서 이미 생성됨 — SPEC 9개 도메인 = Admin(M1) + 8개(M2).

## 검증 + 셀프 리뷰

**성격:** config성(엔티티 매핑 정의, 비즈니스 로직 없음) → 인라인 셀프 리뷰. `/code-review` 미실행.

| 점검 | 결과 |
|------|------|
| 컬럼명·타입 V1 스키마 정합 | ✅ 8개 테이블 전부 일치 (`V1__init.sql`과 대조) |
| 감사 컬럼 위치 | ✅ Question/QuestionOption/Answer 없음 · Analysis `created_at`만 · 나머지 `created_at`+`updated_at` (스키마 동일) |
| Analysis JSONB 3개 | ✅ `@JdbcTypeCode(SqlTypes.JSON)` + hibernate import (hypersistence 미사용 — PLAN 주의사항 반영) |
| `./gradlew compileKotlin` | ✅ BUILD SUCCESSFUL |
| `./gradlew test` (전체) | ✅ BUILD SUCCESSFUL — M1 7개 테스트 회귀 0 |

**핵심 검증 — 스키마 매핑:** `application-test.yaml`의 `ddl-auto=validate`로 기존 M1 통합 테스트가 컨텍스트를 부팅할 때 Hibernate가 **새 엔티티 8개를 포함한 전체 매핑을 V1 스키마와 검증**한다. 전체 테스트 GREEN = 컬럼명/타입/JSONB 매핑이 실제 스키마와 일치함을 의미(컴파일만으로는 잡지 못하는 매핑 오류를 Task 2.4까지 미루지 않고 지금 확인).

## 결정 / 이탈 사항

- **검증 강화**: PLAN Step 7은 `compileKotlin`만 요구하나, 추가로 전체 `test`를 실행해 `ddl-auto=validate` 기반 스키마 매핑 검증까지 수행. (출력 변경 아님, 검증 추가)
- DB의 `CHECK` 제약(`target_count > 0`, answers 단일값)은 엔티티에 표현하지 않음 — 입력 검증은 DTO(`@Min`)·서비스 레이어, DB CHECK은 최종 방어선. PLAN·SPEC 정책과 동일.
- ADR 트리거 없음.
