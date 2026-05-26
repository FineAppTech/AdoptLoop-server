# AdoptLoop — 기술 스택

> [`AdoptLoop_SPEC.md`](./AdoptLoop_SPEC.md)에서 "기술 스택 결정 시 정한다"로 미뤘던 항목을 확정한 문서. 결정 이유와 검토한 대안을 함께 남긴다.

---

## 1. 요약

| 영역 | 선택 |
|---|---|
| 언어 / 런타임 | Kotlin 2.2.21 / Java 21 |
| 프레임워크 | Spring Boot 4.0.6 (Web MVC, Servlet 스택) |
| 프론트엔드 | Thymeleaf(관리자) + Vanilla JS(응답자) |
| DB | PostgreSQL |
| ORM | Spring Data JPA + Hibernate 6 |
| 마이그레이션 | Flyway |
| AI | AWS Bedrock · Claude Haiku 4.5 · Spring AI (Converse API) |
| 로컬 DB 기동 | Spring Boot Docker Compose 지원 (`compose.yaml` 자동) |
| 테스트 | JUnit5 + MockK + MockMvc + Testcontainers(`@ServiceConnection`) + kotlin-test |
| JSON 직렬화 | Jackson 3.x Kotlin 모듈 |
| Slack Webhook | Spring `RestClient` (별도 의존성 없음) |
| 배포 | AWS ECR (이미지 레지스트리) + AWS ECS (Fargate) |

---

## 2. 결정 사항과 이유

### 2.1 프론트엔드 — Thymeleaf + Vanilla JS
- **이유**: 응답자 UI는 토큰·`localStorage` 흐름 때문에 클라이언트 JS가 어차피 필요하지만, 폼·테이블 위주의 관리자 UI는 서버 렌더로 충분. 단일 배포·단일 빌드로 7일 일정에 가장 안전.
- **대안**: Full SPA(React 등) — UI 천장은 높지만 별도 코드베이스·빌드 파이프라인이 일정에 부담. / 최소 폴백(Swagger UI + 정적 HTML) — 시연성 희생이 너무 큼.

### 2.2 DB 액세스 — Spring Data JPA + Hibernate 6
- **이유**: 9개 테이블 대부분이 단순 CRUD라 Repository 인터페이스로 가장 빠르게 처리. JSONB는 `@JdbcTypeCode(SqlTypes.JSON)`로, 집계는 `@Query` 네이티브 SQL로 풀 수 있어 약점이 좁다.
- **다형 `answers` 처리**: 단일 엔티티 + nullable 필드 3개. DB 레벨 `CHECK` 제약은 [도메인 모델 SPEC](./AdoptLoop_SPEC.md#9-도메인-모델)의 후속 정리에 따라 추가 여부 결정.
- **대안**: JdbcClient — 마법은 없지만 CRUD 보일러플레이트가 9개 × 모두 / jOOQ — 타입 안전 SQL은 매력적이지만 빌드 코드 생성이 토이 일정에 과함.

### 2.3 마이그레이션 — Flyway
- **이유**: PostgreSQL 단일 타겟. `V1__init.sql` 같은 순수 SQL 파일 + Spring Boot 자동 통합이면 충분.
- **대안**: Liquibase — 다중 DB 추상화·롤백 명령 등은 MVP에 불필요.

### 2.4 AI 호출 — Bedrock + Claude Haiku 4.5 + Spring AI
- **모델 — Haiku 4.5 단일**: 설문 생성·분석 두 용도 모두 비용·지연·한국어/JSON 안정성에서 합리적. 분석 품질 부족이 관측되면 분석만 Sonnet 4.6으로 분기.
- **호출 — Spring AI (Converse API)**: Spring 생태계와 통합된 `ChatClient` 추상화 사용. 모델 교체 시 코드 변경 최소화.
- **자격**: 배포는 IAM 역할, 로컬은 환경변수/`~/.aws` 프로파일(SPEC 8.3과 동일).
- **대안**: AWS SDK v2 직접 호출 — 의존성 최소·명시적이지만 모델 추상화/구조화 출력 헬퍼를 직접 구현해야 함. / LangChain4j — Spring 통합도가 Spring AI보다 약함.

### 2.5 로컬 개발·테스트 DB
- **개발 — Spring Boot Docker Compose 지원**: 루트의 `compose.yaml`을 부팅 시 자동 인식. 종료 시 정리. 별도 명령 불필요.
- **테스트 — Testcontainers + PostgreSQL 이미지**: `@SpringBootTest` + `@ServiceConnection`으로 실제 PG에서 통합 테스트. JSONB·`CHECK`까지 프로덕션과 동일 동작.
- **대안**: H2 — JSONB 미지원으로 즉시 탈락 / 수동 Docker Compose — 자동 기동이 없는 만큼 마찰만 늘어남.

### 2.6 테스트 — JUnit5 + MockK + MockMvc + Testcontainers
- **이유**: Kotlin 친화(`final by default`)·코루틴 친화로 MockK가 표준. MockMvc는 이미 의존성 포함. 통합 테스트는 Testcontainers로 실제 PG. 단언은 `kotlin-test`로 충분, 모자라면 AssertJ 추후 추가.

### 2.7 배포 — AWS ECR + ECS (Fargate)
- **흐름**: 로컬/CI에서 `docker build` → **AWS ECR**에 이미지 push → **AWS ECS** 서비스가 새 task revision으로 롤링 업데이트.
- **런치 타입 — Fargate**: EC2 노드를 직접 관리하지 않는다. 토이 일정에 운영 부담 최소.
- **IAM**: ECS Task Role로 Bedrock 호출 권한 부여 (SPEC 8.3 "배포 환경에서는 IAM 역할"의 구체 구현 지점).
- **시크릿**: Slack Webhook URL 등은 ECS task 환경변수 또는 AWS SSM Parameter Store / Secrets Manager로 주입. 이미지·코드에 평문 포함 금지.
- **DB**: PostgreSQL은 동일 VPC의 RDS PostgreSQL을 가정. 토이라면 단일 인스턴스(`db.t4g.micro` 수준)면 충분.
- **CI/CD**: 도구(GitHub Actions / CodeBuild / 수동 push)는 별도 결정. MVP 단계에선 수동 push도 가능.
- **대안**: ECS EC2 런치 타입 — 비용 최적화 가능하지만 노드 관리 부담. / EKS — Kubernetes 학습 비용으로 토이엔 과함. / App Runner — 더 간단하지만 ECS 경험 축적 목적이면 ECS가 적합.

---

## 3. 추가될 build.gradle.kts 의존성

> 버전은 Spring Boot BOM이 관리하는 것은 명시하지 않고, Spring AI·Testcontainers·MockK 등 BOM 외 항목만 명시 예정. `writing-plans` 단계에서 실제 좌표·버전 확정.

- 프론트 렌더링: `spring-boot-starter-thymeleaf`
- DB 액세스: `spring-boot-starter-data-jpa`, `org.postgresql:postgresql`
- Kotlin JPA: `kotlin("plugin.jpa")` (no-arg 자동 생성)
- Validation: `spring-boot-starter-validation`
- 마이그레이션: `org.flywaydb:flyway-core`, `org.flywaydb:flyway-database-postgresql`
- 로컬 DB 기동: `spring-boot-docker-compose`
- AI: Spring AI BOM + `spring-ai-starter-model-bedrock-converse`
- 테스트:
  - `io.mockk:mockk`
  - `com.ninja-squad:springmockk` (Spring 빈 MockK 통합, 선택)
  - `org.springframework.boot:spring-boot-testcontainers`
  - `org.testcontainers:postgresql`

---

## 4. 후속 결정 (writing-plans 또는 구현 중)

- Spring AI BOM 정확 버전 / `spring-ai-starter-*` 정확 좌표
- `compose.yaml`의 PostgreSQL 이미지 태그 (예: `postgres:16-alpine`)
- 슬랙 전송 실패 처리(재시도 vs 무시) 정책
- `updated_at` 자동 갱신 방식 (JPA `@UpdateTimestamp` vs DB 트리거)
- `application.yaml` 프로파일 분리 (`local` / `test` / `prod`) 필요 여부
- Dockerfile 구성 (multi-stage, JVM 베이스 이미지 선택)
- CI/CD 도구 결정 (GitHub Actions / CodeBuild / 수동)
- ECS Service / Task Definition 정의 (오토스케일링·헬스체크 경로 등)
- RDS PostgreSQL 인스턴스 사양 및 VPC 구성

---

## 변경 이력

| 날짜 | 내용 |
|---|---|
| 2026-05-26 | 초안 작성 — SPEC에서 보류했던 모든 기술 스택 항목 확정 |
| 2026-05-26 | 배포 항목 추가 — AWS ECR + ECS (Fargate) |
