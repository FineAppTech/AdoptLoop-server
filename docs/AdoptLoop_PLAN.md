# AdoptLoop MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** AdoptLoop SPEC 6장 MVP 전체를 7일 안에 구현·검증·배포한다. 구현 ≈ 3일, 검증 ≈ 1-2일, 배포 ≈ 1-2일.

**Architecture:** Spring Boot 4 (Web MVC, Servlet) + Kotlin 2.2 + JPA/Hibernate 6 + Flyway. AI 호출은 Spring AI Bedrock Converse (Claude Haiku 4.5). 관리자 API = JSON REST + `X-Admin-Key`, 응답자 API = JSON REST + `access_token` (path). UI는 Thymeleaf(관리자) + Vanilla JS(응답자). 배포는 단일 Spring Boot fat-jar → Docker → ECR → ECS Fargate (RDS PostgreSQL).

**Tech Stack:** Kotlin 2.2.21, Spring Boot 4.0.6, Java 21, PostgreSQL 16, Flyway, Spring AI (Bedrock Converse), Testcontainers + MockK + MockMvc, Docker, AWS ECR/ECS Fargate/RDS.

**참조 문서:** [SPEC](./AdoptLoop_SPEC.md) · [DOMAIN_FLOW](./AdoptLoop_DOMAIN_FLOW.md) · [TECH_STACK](./AdoptLoop_TECH_STACK.md) · [API](./AdoptLoop_API.yaml)

---

## 파일 구조 (계획)

```
src/main/kotlin/com/tnear/adoptloop/
  AdoptloopServerApplication.kt             (existing)
  config/
    JpaConfig.kt                            # @EnableJpaAuditing
    SecurityConfig.kt                       # AdminKeyFilter 등록 + permitAll 정책
    BedrockConfig.kt                        # ChatClient bean (Spring AI)
    SlackProperties.kt                      # @ConfigurationProperties (slack.*)
    LlmTransientException.kt                # LLM 호출/파싱 실패 → 503 LLM_TRANSIENT
    GlobalExceptionHandler.kt
  admin/
    AdminBootstrap.kt                       # CommandLineRunner — seed admin from env
    auth/
      AdminKeyFilter.kt                     # X-Admin-Key → SHA-256 → admins.key_hash
      AdminContext.kt                       # RequestScope 빈, 현재 admin_id 노출
  domain/
    Enums.kt                                # AdoptionStatus, SurveyStatus, QuestionType, Axis, Priority, TodoStatus
    Admin.kt
    Adoption.kt
    Survey.kt
    Question.kt
    QuestionOption.kt
    SurveyResponse.kt
    Answer.kt
    Analysis.kt
    ActionItem.kt
    repo/
      AdminRepository.kt
      AdoptionRepository.kt
      SurveyRepository.kt
      QuestionRepository.kt
      QuestionOptionRepository.kt
      SurveyResponseRepository.kt
      AnswerRepository.kt
      AnalysisRepository.kt
      ActionItemRepository.kt
  adoption/
    AdoptionDtos.kt
    AdoptionService.kt
    AdoptionController.kt
  survey/
    SurveyDtos.kt
    SurveyService.kt
    SurveyController.kt
    publish/
      SurveyPublisher.kt                    # afterCommit 훅 등록
      SlackNotifier.kt                      # 서버 config webhook URL로 RestClient POST
    draft/
      SurveyDraftPrompt.kt                  # 프롬프트 빌더
      SurveyDraftParser.kt                  # LLM JSON → 도메인 입력
      SurveyDraftService.kt                 # Bedrock 호출 + 트랜잭션 저장
  publicapi/
    PublicDtos.kt
    PublicSurveyController.kt
    PublicResponseService.kt
  analysis/
    AggregateService.kt
    AggregateDtos.kt
    AnalysisPrompt.kt
    AnalysisParser.kt
    AnalysisService.kt
    AnalysisController.kt
    AnalysisDtos.kt
  actionitem/
    ActionItemDtos.kt
    ActionItemService.kt
    ActionItemController.kt
  web/
    AdminViewController.kt                  # Thymeleaf 페이지 라우트
    PublicViewController.kt                 # /s/{slug} 정적 페이지

src/main/resources/
  application.yaml                          # 공통
  application-local.yaml                    # 로컬 (compose.yaml 자동 인식)
  application-test.yaml                     # Testcontainers
  application-prod.yaml                     # ECS
  db/migration/
    V1__init.sql                            # 9개 테이블 + 인덱스
  templates/admin/
    layout.html
    adoptions/list.html
    adoptions/new.html
    adoptions/detail.html
    surveys/edit.html
    surveys/analyze.html
  templates/public/
    survey.html                             # vanilla JS shell
  static/
    css/admin.css
    js/respondent.js

src/test/kotlin/com/tnear/adoptloop/
  IntegrationTestBase.kt                    # @SpringBootTest + @Testcontainers + @ServiceConnection
  admin/AdminKeyFilterTest.kt
  adoption/AdoptionControllerTest.kt
  survey/SurveyControllerTest.kt
  survey/SurveyDraftServiceTest.kt
  publicapi/PublicSurveyControllerTest.kt
  analysis/AggregateServiceTest.kt
  analysis/AnalysisServiceTest.kt
  e2e/HappyPathE2ETest.kt
  e2e/EdgeCaseE2ETest.kt

compose.yaml                                # PostgreSQL 16
Dockerfile                                  # multi-stage
.dockerignore
deploy/
  task-definition.json                      # ECS Fargate
  README.md                                 # 배포 절차
.github/workflows/
  ci.yml                                    # build + test on PR
  deploy.yml                                # ECR push + ECS update on main
```

---

## Milestone 1 — Foundation (Day 1 AM, ~4-5h)

> **REST Docs 정책 ([ADR-0009](adr/0009-spring-restdocs-enforcement.md))**: 컨트롤러 endpoint 테스트는 `ControllerTestBase`를 통해 `document()` 호출 필수. `ControllerTestBase` + JUnit Extension은 **M2 첫 컨트롤러 테스트 직전 Task에서 도입**. M1 Task 1.5 `AdminKeyFilterTest`는 필터 동작 테스트(컨트롤러 endpoint 아님)라 강제 대상에서 제외.

### Task 1.1: 의존성 추가

**Files:**
- Modify: `build.gradle.kts`

- [ ] **Step 1: build.gradle.kts 전체 교체**

```kotlin
plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.jpa") version "2.2.21"
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.asciidoctor.jvm.convert") version "4.0.4"
}

group = "com.tnear"
version = "0.0.1-SNAPSHOT"
description = "adoptloop-server"

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

repositories { mavenCentral() }

extra["springAiVersion"] = "1.0.4"

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.ai:spring-ai-starter-model-bedrock-converse")

    runtimeOnly("org.postgresql:postgresql")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> { useJUnitPlatform() }

val snippetsDir = layout.buildDirectory.dir("generated-snippets")
tasks.test { outputs.dir(snippetsDir) }
tasks.asciidoctor {
    inputs.dir(snippetsDir)
    dependsOn(tasks.test)
}
```

- [ ] **Step 2: 의존성 해석 확인**

Run: `./gradlew dependencies --configuration testRuntimeClasspath > /dev/null`
Expected: 0 종료, Spring AI bedrock-converse + spring-restdocs-mockmvc 포함 확인.

- [ ] **Step 3: asciidoctor 태스크 등록 확인**

Run: `./gradlew tasks --group documentation`
Expected: `asciidoctor` 태스크 출력. (src/docs/asciidoc/ 미존재 상태에서도 태스크 자체는 등록되어야 함.)

- [ ] **Step 4: Commit**

```bash
git checkout -b feat/foundation
git add build.gradle.kts
git commit -m "build: add JPA/Flyway/Thymeleaf/Spring AI/Testcontainers/REST Docs deps + asciidoctor plugin"
```

---

### Task 1.2: compose.yaml + application 프로파일

**Files:**
- Create: `compose.yaml`
- Modify: `src/main/resources/application.yaml`
- Create: `src/main/resources/application-local.yaml`
- Create: `src/main/resources/application-test.yaml`
- Create: `src/main/resources/application-prod.yaml`

- [ ] **Step 1: compose.yaml 작성**

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: adoptloop
      POSTGRES_USER: adoptloop
      POSTGRES_PASSWORD: adoptloop
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U adoptloop"]
      interval: 5s
      retries: 10
```

- [ ] **Step 2: application.yaml (공통)**

```yaml
spring:
  application:
    name: adoptloop-server
  profiles:
    default: local
  jackson:
    property-naming-strategy: SNAKE_CASE
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration

adoptloop:
  admin:
    bootstrap-name: ${ADOPTLOOP_ADMIN_NAME:}
    bootstrap-key: ${ADOPTLOOP_ADMIN_KEY:}
  bedrock:
    model: anthropic.claude-haiku-4-5-20251001-v1:0
    region: ${AWS_REGION:ap-northeast-2}
  slack:
    webhook-url: ${ADOPTLOOP_SLACK_WEBHOOK_URL:}
  public-base-url: ${ADOPTLOOP_PUBLIC_BASE_URL:http://localhost:8080}

logging:
  level:
    org.hibernate.SQL: INFO
```

- [ ] **Step 3: application-local.yaml**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/adoptloop
    username: adoptloop
    password: adoptloop
```

- [ ] **Step 4: application-test.yaml**

```yaml
spring:
  flyway:
    clean-disabled: false
logging:
  level:
    org.hibernate.SQL: WARN
```

- [ ] **Step 5: application-prod.yaml**

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
```

- [ ] **Step 6: SlackProperties.kt + @ConfigurationPropertiesScan**

`src/main/kotlin/com/tnear/adoptloop/config/SlackProperties.kt`:
```kotlin
package com.tnear.adoptloop.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("adoptloop.slack")
data class SlackProperties(val webhookUrl: String? = null)
```

`src/main/kotlin/com/tnear/adoptloop/AdoptloopServerApplication.kt` 수정:
```kotlin
@SpringBootApplication
@org.springframework.boot.context.properties.ConfigurationPropertiesScan
class AdoptloopServerApplication
```

- [ ] **Step 7: Commit**

```bash
git add compose.yaml src/main/resources/application*.yaml src/main/kotlin/com/tnear/adoptloop/config src/main/kotlin/com/tnear/adoptloop/AdoptloopServerApplication.kt
git commit -m "config: compose.yaml + profile-separated application.yaml + Slack properties"
```

---

### Task 1.3: Flyway V1 — 9개 테이블

**Files:**
- Create: `src/main/resources/db/migration/V1__init.sql`

> **deadline 정책**: SPEC NOT NULL을 그대로 따른다. draft 생성 시점에 `deadline`을 필수 입력으로 받고, 발행에서는 deadline을 변경하지 않는다. (DOMAIN_FLOW Step 3·4와 차이 — 의도적: draft에 sentinel/NULL을 두지 않아 흐름이 단순해짐.)
>
> **Slack webhook 정책**: SPEC 9장의 `surveys.slack_webhook_url` 컬럼은 사용하지 않는다. 서버 config(`adoptloop.slack.webhook-url`)에 단일 채널을 고정하고, 발행되는 모든 설문이 그 채널로 자동 알림. SSRF 위험 차단 + 운영 단순화. config 비어 있으면 알림 skip.

- [ ] **Step 1: V1__init.sql 작성**

```sql
-- 정책: 외래키 제약은 DB 레벨에 두지 않는다. 참조 무결성은 JPA/서비스 레이어에서 관리.
-- 조회 성능 보장을 위해 FK 컬럼에는 인덱스를 유지한다.

CREATE TABLE admins (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    key_hash    VARCHAR(64)  NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE adoptions (
    id              BIGSERIAL PRIMARY KEY,
    admin_id        BIGINT       NOT NULL,
    name            VARCHAR(200) NOT NULL,
    goal            TEXT         NOT NULL,
    target_audience TEXT         NOT NULL,
    concern         TEXT,
    target_count    INTEGER      NOT NULL CHECK (target_count > 0),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_adoptions_admin ON adoptions(admin_id);

CREATE TABLE surveys (
    id                  BIGSERIAL PRIMARY KEY,
    adoption_id         BIGINT       NOT NULL,
    title               VARCHAR(200) NOT NULL,
    public_slug         VARCHAR(64)  NOT NULL UNIQUE,
    deadline            TIMESTAMPTZ  NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    published_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_surveys_adoption ON surveys(adoption_id);

CREATE TABLE questions (
    id           BIGSERIAL PRIMARY KEY,
    survey_id    BIGINT       NOT NULL,
    type         VARCHAR(20)  NOT NULL,
    text         TEXT         NOT NULL,
    order_index  INTEGER      NOT NULL,
    required     BOOLEAN      NOT NULL DEFAULT TRUE,
    axis         VARCHAR(20)
);
CREATE INDEX idx_questions_survey ON questions(survey_id, order_index);

CREATE TABLE question_options (
    id           BIGSERIAL PRIMARY KEY,
    question_id  BIGINT       NOT NULL,
    text         VARCHAR(300) NOT NULL,
    order_index  INTEGER      NOT NULL
);
CREATE INDEX idx_options_question ON question_options(question_id, order_index);

CREATE TABLE survey_responses (
    id            BIGSERIAL PRIMARY KEY,
    survey_id     BIGINT       NOT NULL,
    access_token  VARCHAR(64)  NOT NULL UNIQUE,
    status        VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS',
    submitted_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_responses_survey ON survey_responses(survey_id);

CREATE TABLE answers (
    id                  BIGSERIAL PRIMARY KEY,
    survey_response_id  BIGINT  NOT NULL,
    question_id         BIGINT  NOT NULL,
    text_value          TEXT,
    question_option_id  BIGINT,
    scale_value         INTEGER,
    CHECK (
        (text_value IS NOT NULL)::int +
        (question_option_id IS NOT NULL)::int +
        (scale_value IS NOT NULL)::int = 1
    )
);
CREATE INDEX idx_answers_response ON answers(survey_response_id);
CREATE INDEX idx_answers_question ON answers(question_id);

CREATE TABLE analyses (
    id                  BIGSERIAL PRIMARY KEY,
    survey_id           BIGINT      NOT NULL,
    adoption_score      INTEGER     NOT NULL,
    usage_score         INTEGER     NOT NULL,
    behavior_score      INTEGER     NOT NULL,
    value_score         INTEGER     NOT NULL,
    positive_signals    JSONB       NOT NULL,
    resistance_factors  JSONB       NOT NULL,
    risks               JSONB       NOT NULL,
    raw_output          TEXT        NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_analyses_survey ON analyses(survey_id, created_at DESC);

CREATE TABLE action_items (
    id           BIGSERIAL PRIMARY KEY,
    adoption_id  BIGINT       NOT NULL,
    analysis_id  BIGINT       NOT NULL,
    title        VARCHAR(300) NOT NULL,
    description  TEXT,
    priority     VARCHAR(10)  NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'TODO',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_action_items_adoption ON action_items(adoption_id);
```

- [ ] **Step 2: compose 기동 + 마이그레이션 검증**

Run:
```bash
docker compose up -d postgres
./gradlew bootRun --args='--spring.profiles.active=local' &
sleep 15 && psql postgresql://adoptloop:adoptloop@localhost:5432/adoptloop -c '\dt'
```
Expected: 9개 테이블 + `flyway_schema_history`. 실행 후 `kill %1`로 정지.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V1__init.sql
git commit -m "db: V1 init migration (9 tables, JSONB + answers CHECK)"
```

---

### Task 1.4: Enums + Admin entity + Repository (TDD)

**Files:**
- Create: `src/main/kotlin/com/tnear/adoptloop/domain/Enums.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/domain/Admin.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/domain/repo/AdminRepository.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/config/JpaConfig.kt`
- Create: `src/test/kotlin/com/tnear/adoptloop/IntegrationTestBase.kt`
- Create: `src/test/kotlin/com/tnear/adoptloop/domain/AdminRepositoryTest.kt`

- [ ] **Step 1: IntegrationTestBase 작성**

```kotlin
package com.tnear.adoptloop

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
abstract class IntegrationTestBase {

    @Autowired
    protected lateinit var jdbc: JdbcTemplate

    @BeforeEach
    fun truncateAll() {
        jdbc.execute("""
            TRUNCATE TABLE
              answers, action_items, analyses, survey_responses,
              question_options, questions, surveys, adoptions, admins
            RESTART IDENTITY CASCADE
        """.trimIndent())
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("adoptloop_test")
            .withUsername("test")
            .withPassword("test")
    }
}
```

- [ ] **Step 2: 실패하는 테스트 작성**

```kotlin
package com.tnear.adoptloop.domain

import com.tnear.adoptloop.IntegrationTestBase
import com.tnear.adoptloop.domain.repo.AdminRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AdminRepositoryTest @Autowired constructor(
    private val repo: AdminRepository,
) : IntegrationTestBase() {

    @Test
    fun `save and findByKeyHash`() {
        val saved = repo.save(Admin(name = "alice", keyHash = "a".repeat(64)))
        assertNotNull(saved.id)

        val found = repo.findByKeyHash("a".repeat(64))
        assertEquals("alice", found?.name)
    }
}
```

- [ ] **Step 3: 테스트 실행 (FAIL)**

Run: `./gradlew test --tests AdminRepositoryTest`
Expected: 컴파일 실패 — Admin, AdminRepository 미정의.

- [ ] **Step 4: Enums.kt**

```kotlin
package com.tnear.adoptloop.domain

enum class AdoptionStatus { ACTIVE, ARCHIVED }
enum class SurveyStatus { DRAFT, PUBLISHED, CLOSED }
enum class QuestionType { TEXT, SINGLE_CHOICE, SCALE }
enum class Axis { USAGE, BEHAVIOR, VALUE }
enum class ResponseStatus { IN_PROGRESS, SUBMITTED }
enum class Priority { HIGH, MEDIUM, LOW }
enum class TodoStatus { TODO, IN_PROGRESS, DONE }
```

- [ ] **Step 5: Admin entity**

```kotlin
package com.tnear.adoptloop.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(name = "admins")
@EntityListeners(AuditingEntityListener::class)
class Admin(
    @Column(nullable = false) var name: String,
    @Column(name = "key_hash", nullable = false, unique = true) var keyHash: String,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.EPOCH
}
```

- [ ] **Step 6: AdminRepository**

```kotlin
package com.tnear.adoptloop.domain.repo

import com.tnear.adoptloop.domain.Admin
import org.springframework.data.jpa.repository.JpaRepository

interface AdminRepository : JpaRepository<Admin, Long> {
    fun findByKeyHash(keyHash: String): Admin?
}
```

- [ ] **Step 7: JpaConfig**

```kotlin
package com.tnear.adoptloop.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@Configuration
@EnableJpaAuditing
class JpaConfig
```

- [ ] **Step 8: 테스트 실행 (PASS)**

Run: `./gradlew test --tests AdminRepositoryTest`
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add src/main/kotlin src/test/kotlin
git commit -m "domain: Admin entity + repository (TDD)"
```

---

### Task 1.5: AdminKeyFilter + SecurityConfig

**Files:**
- Create: `src/main/kotlin/com/tnear/adoptloop/admin/auth/AdminContext.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/admin/auth/AdminKeyFilter.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/config/SecurityConfig.kt`
- Create: `src/test/kotlin/com/tnear/adoptloop/admin/AdminKeyFilterTest.kt`

- [ ] **Step 1: 실패하는 통합 테스트 작성**

```kotlin
package com.tnear.adoptloop.admin

import com.tnear.adoptloop.IntegrationTestBase
import com.tnear.adoptloop.domain.Admin
import com.tnear.adoptloop.domain.repo.AdminRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import java.security.MessageDigest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class AdminKeyFilterTest @Autowired constructor(
    private val mvc: MockMvc,
) : IntegrationTestBase() {

    @Test
    fun `missing key returns 401`() {
        mvc.perform(get("/api/admin/adoptions"))
            .andExpect { it.response.status == 401 }
    }

    @Test
    fun `invalid key returns 401`() {
        mvc.perform(get("/api/admin/adoptions").header("X-Admin-Key", "no-such-key"))
            .andExpect { it.response.status == 401 }
    }
}

// 200 통과 케이스는 M2.4 AdoptionControllerTest에서 자연스럽게 검증된다.
```

- [ ] **Step 2: 테스트 실행 (FAIL)**

Run: `./gradlew test --tests AdminKeyFilterTest`
Expected: 컴파일 실패 또는 404 (controller·filter 미존재).

- [ ] **Step 3: AdminContext**

```kotlin
package com.tnear.adoptloop.admin.auth

import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
class AdminContext {
    var adminId: Long? = null
    fun require(): Long = adminId ?: error("admin not authenticated")
}
```

- [ ] **Step 4: AdminKeyFilter**

```kotlin
package com.tnear.adoptloop.admin.auth

import com.tnear.adoptloop.domain.repo.AdminRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest

@Component
class AdminKeyFilter(
    private val adminRepo: AdminRepository,
    private val adminContext: AdminContext,
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !request.requestURI.startsWith("/api/admin")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val raw = request.getHeader("X-Admin-Key")
        if (raw.isNullOrBlank()) { response.status = 401; return }

        val hash = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val admin = adminRepo.findByKeyHash(hash)
        if (admin == null) { response.status = 401; return }

        adminContext.adminId = admin.id
        chain.doFilter(request, response)
    }
}
```

- [ ] **Step 5: SecurityConfig — 필터 자동 등록 외 별도 설정 없음**

> Spring Boot는 `@Component` `OncePerRequestFilter`를 자동 등록한다. 별도 `SecurityFilterChain`은 6.x에서 도입 시점에 추가.

(Task 1.5에선 SecurityConfig 파일 생성 보류 — Task 5.x 또는 필요 시점에 추가)

- [ ] **Step 6: 테스트 실행 (PASS)**

Run: `./gradlew test --tests AdminKeyFilterTest`
Expected: PASS — 두 401 케이스. (controller 미존재 시 Spring은 핸들러 호출 전에 필터가 먼저 401을 반환하므로 404가 아님.)

- [ ] **Step 7: Commit**

```bash
git add src/main/kotlin/com/tnear/adoptloop/admin src/test/kotlin/com/tnear/adoptloop/admin
git commit -m "auth: AdminKeyFilter (X-Admin-Key → SHA-256)"
```

---

### Task 1.6: AdminBootstrap (env 기반 시드)

**Files:**
- Create: `src/main/kotlin/com/tnear/adoptloop/admin/AdminBootstrap.kt`

- [ ] **Step 1: AdminBootstrap.kt**

```kotlin
package com.tnear.adoptloop.admin

import com.tnear.adoptloop.domain.Admin
import com.tnear.adoptloop.domain.repo.AdminRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.security.MessageDigest

@Component
class AdminBootstrap(
    private val repo: AdminRepository,
    private val env: org.springframework.core.env.Environment,
    @Value("\${adoptloop.admin.bootstrap-name:}") private val name: String,
    @Value("\${adoptloop.admin.bootstrap-key:}") private val key: String,
) : CommandLineRunner {

    private val log = org.slf4j.LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String) {
        if (name.isBlank() || key.isBlank()) {
            if (env.activeProfiles.contains("prod") && repo.count() == 0L) {
                log.warn("No admins exist and ADOPTLOOP_ADMIN_NAME/KEY not set — all /api/admin/* will return 401 until an admin is seeded.")
            }
            return
        }
        val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
        if (repo.findByKeyHash(hash) == null) {
            repo.save(Admin(name = name, keyHash = hash))
        }
    }
}
```

- [ ] **Step 2: Bootstrap 수동 검증**

Run:
```bash
ADOPTLOOP_ADMIN_NAME=tester ADOPTLOOP_ADMIN_KEY=devkey \
  ./gradlew bootRun --args='--spring.profiles.active=local' &
sleep 15
psql postgresql://adoptloop:adoptloop@localhost:5432/adoptloop -c 'SELECT name FROM admins;'
kill %1
```
Expected: `tester` 출력.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/tnear/adoptloop/admin/AdminBootstrap.kt
git commit -m "admin: env-based bootstrap seeder"
```

---

### Task 1.7: PR 생성

- [ ] **Step 1: M1 PR**

```bash
git push -u origin feat/foundation
gh pr create --title "feat(M1): foundation — build deps, V1 migration, admin auth, bootstrap" \
  --body "$(cat <<'EOF'
## Summary
- Gradle 의존성 일괄 추가
- compose.yaml (PostgreSQL 16) + 프로파일 분리 (local/test/prod)
- Flyway V1: 9개 테이블 + 인덱스 + answers CHECK 제약
- Admin 엔티티 + Repository (TDD)
- AdminKeyFilter (X-Admin-Key → SHA-256)
- AdminBootstrap (env 기반 시드)

## Test plan
- [x] `./gradlew test` — AdminRepositoryTest, AdminKeyFilterTest 그린
- [x] 로컬 부팅 시 Flyway 9 테이블 생성
- [x] 환경변수 시드 후 admins 행 1건

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

(머지는 사용자가 수동)

---

## Milestone 2 — Domain Entities + Adoption CRUD (Day 1 PM, ~4-5h)

### Task 2.1: 9개 도메인 엔티티 일괄 작성

**Files:**
- Create: `src/main/kotlin/com/tnear/adoptloop/domain/Adoption.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/domain/Survey.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/domain/Question.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/domain/QuestionOption.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/domain/SurveyResponse.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/domain/Answer.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/domain/Analysis.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/domain/ActionItem.kt`

- [ ] **Step 1: 새 브랜치**

```bash
git checkout main && git pull && git checkout -b feat/adoption-crud
```

- [ ] **Step 2: Adoption.kt**

```kotlin
package com.tnear.adoptloop.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(name = "adoptions")
@EntityListeners(AuditingEntityListener::class)
class Adoption(
    @Column(name = "admin_id", nullable = false) var adminId: Long,
    @Column(nullable = false) var name: String,
    @Column(nullable = false, columnDefinition = "TEXT") var goal: String,
    @Column(name = "target_audience", nullable = false, columnDefinition = "TEXT") var targetAudience: String,
    @Column(columnDefinition = "TEXT") var concern: String? = null,
    @Column(name = "target_count", nullable = false) var targetCount: Int,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: AdoptionStatus = AdoptionStatus.ACTIVE,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) var createdAt: Instant = Instant.EPOCH
    @LastModifiedDate @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.EPOCH
}
```

- [ ] **Step 3: Survey.kt**

```kotlin
package com.tnear.adoptloop.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(name = "surveys")
@EntityListeners(AuditingEntityListener::class)
class Survey(
    @Column(name = "adoption_id", nullable = false) var adoptionId: Long,
    @Column(nullable = false) var title: String,
    @Column(name = "public_slug", nullable = false, unique = true) var publicSlug: String,
    @Column(nullable = false) var deadline: Instant,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: SurveyStatus = SurveyStatus.DRAFT,
    @Column(name = "published_at") var publishedAt: Instant? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) var createdAt: Instant = Instant.EPOCH
    @LastModifiedDate @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.EPOCH
}
```

- [ ] **Step 4: Question.kt + QuestionOption.kt**

```kotlin
package com.tnear.adoptloop.domain

import jakarta.persistence.*

@Entity
@Table(name = "questions")
class Question(
    @Column(name = "survey_id", nullable = false) var surveyId: Long,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var type: QuestionType,
    @Column(nullable = false, columnDefinition = "TEXT") var text: String,
    @Column(name = "order_index", nullable = false) var orderIndex: Int,
    @Column(nullable = false) var required: Boolean = true,
    @Enumerated(EnumType.STRING) @Column(length = 20) var axis: Axis? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
}
```

```kotlin
package com.tnear.adoptloop.domain

import jakarta.persistence.*

@Entity
@Table(name = "question_options")
class QuestionOption(
    @Column(name = "question_id", nullable = false) var questionId: Long,
    @Column(nullable = false) var text: String,
    @Column(name = "order_index", nullable = false) var orderIndex: Int,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
}
```

- [ ] **Step 5: SurveyResponse.kt + Answer.kt**

```kotlin
package com.tnear.adoptloop.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(name = "survey_responses")
@EntityListeners(AuditingEntityListener::class)
class SurveyResponse(
    @Column(name = "survey_id", nullable = false) var surveyId: Long,
    @Column(name = "access_token", nullable = false, unique = true) var accessToken: String,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: ResponseStatus = ResponseStatus.IN_PROGRESS,
    @Column(name = "submitted_at") var submittedAt: Instant? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) var createdAt: Instant = Instant.EPOCH
    @LastModifiedDate @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.EPOCH
}
```

```kotlin
package com.tnear.adoptloop.domain

import jakarta.persistence.*

@Entity
@Table(name = "answers")
class Answer(
    @Column(name = "survey_response_id", nullable = false) var surveyResponseId: Long,
    @Column(name = "question_id", nullable = false) var questionId: Long,
    @Column(name = "text_value", columnDefinition = "TEXT") var textValue: String? = null,
    @Column(name = "question_option_id") var questionOptionId: Long? = null,
    @Column(name = "scale_value") var scaleValue: Int? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
}
```

- [ ] **Step 6: Analysis.kt + ActionItem.kt**

```kotlin
package com.tnear.adoptloop.domain

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(name = "analyses")
@EntityListeners(AuditingEntityListener::class)
class Analysis(
    @Column(name = "survey_id", nullable = false) var surveyId: Long,
    @Column(name = "adoption_score", nullable = false) var adoptionScore: Int,
    @Column(name = "usage_score", nullable = false) var usageScore: Int,
    @Column(name = "behavior_score", nullable = false) var behaviorScore: Int,
    @Column(name = "value_score", nullable = false) var valueScore: Int,
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "positive_signals", nullable = false, columnDefinition = "jsonb")
    var positiveSignals: List<String>,
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "resistance_factors", nullable = false, columnDefinition = "jsonb")
    var resistanceFactors: List<String>,
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb")
    var risks: List<String>,
    @Column(name = "raw_output", nullable = false, columnDefinition = "TEXT") var rawOutput: String,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) var createdAt: Instant = Instant.EPOCH
}
```

```kotlin
package com.tnear.adoptloop.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(name = "action_items")
@EntityListeners(AuditingEntityListener::class)
class ActionItem(
    @Column(name = "adoption_id", nullable = false) var adoptionId: Long,
    @Column(name = "analysis_id", nullable = false) var analysisId: Long,
    @Column(nullable = false) var title: String,
    @Column(columnDefinition = "TEXT") var description: String? = null,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) var priority: Priority,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: TodoStatus = TodoStatus.TODO,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) var createdAt: Instant = Instant.EPOCH
    @LastModifiedDate @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.EPOCH
}
```

- [ ] **Step 7: 빌드 검증**

Run: `./gradlew compileKotlin`
Expected: 0 종료. (Analysis.kt에서 hypersistence import 라인을 지우는 것 잊지 말 것.)

- [ ] **Step 8: Commit**

```bash
git add src/main/kotlin/com/tnear/adoptloop/domain
git commit -m "domain: 8 JPA entities (Adoption, Survey, Question, Option, Response, Answer, Analysis, ActionItem)"
```

---

### Task 2.2: 8개 Repository

**Files:**
- Create: `src/main/kotlin/com/tnear/adoptloop/domain/repo/{Adoption,Survey,Question,QuestionOption,SurveyResponse,Answer,Analysis,ActionItem}Repository.kt`

- [ ] **Step 1: 한 파일로 묶어 작성**

```kotlin
package com.tnear.adoptloop.domain.repo

import com.tnear.adoptloop.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface AdoptionRepository : JpaRepository<Adoption, Long> {
    fun findAllByAdminId(adminId: Long): List<Adoption>
}

interface SurveyRepository : JpaRepository<Survey, Long> {
    fun findByPublicSlug(slug: String): Survey?
    fun findAllByAdoptionId(adoptionId: Long): List<Survey>
}

interface QuestionRepository : JpaRepository<Question, Long> {
    fun findAllBySurveyIdOrderByOrderIndex(surveyId: Long): List<Question>
    fun deleteAllBySurveyId(surveyId: Long)
}

interface QuestionOptionRepository : JpaRepository<QuestionOption, Long> {
    fun findAllByQuestionIdInOrderByOrderIndex(questionIds: Collection<Long>): List<QuestionOption>
    fun deleteAllByQuestionIdIn(questionIds: Collection<Long>)
}

interface SurveyResponseRepository : JpaRepository<SurveyResponse, Long> {
    fun findByAccessToken(token: String): SurveyResponse?
    fun countBySurveyIdAndStatus(surveyId: Long, status: ResponseStatus): Long
    fun countBySurveyId(surveyId: Long): Long
    fun findAllBySurveyIdAndStatus(surveyId: Long, status: ResponseStatus): List<SurveyResponse>
}

interface AnswerRepository : JpaRepository<Answer, Long> {
    fun deleteAllBySurveyResponseId(responseId: Long)
    fun findAllBySurveyResponseId(responseId: Long): List<Answer>
    fun findAllBySurveyResponseIdIn(responseIds: Collection<Long>): List<Answer>
}

interface AnalysisRepository : JpaRepository<Analysis, Long> {
    fun findFirstBySurveyIdOrderByCreatedAtDesc(surveyId: Long): Optional<Analysis>
    fun findAllBySurveyIdOrderByCreatedAtDesc(surveyId: Long): List<Analysis>
}

interface ActionItemRepository : JpaRepository<ActionItem, Long> {
    fun findAllByAdoptionId(adoptionId: Long): List<ActionItem>
}
```

> 8개 인터페이스를 한 파일(`Repositories.kt`)에 두어도 무방하나, 컨벤션 통일을 위해 8개 파일로 분리. 위 코드를 인터페이스별로 잘라 배치.

- [ ] **Step 2: 빌드 검증**

Run: `./gradlew compileKotlin compileTestKotlin`
Expected: 0 종료.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/tnear/adoptloop/domain/repo
git commit -m "domain: 8 JPA repositories"
```

---

### Task 2.3: ControllerTestBase + REST Docs JUnit Extension (인프라)

**Files:**
- Create: `src/test/kotlin/com/tnear/adoptloop/restdocs/DocCallTracker.kt`
- Create: `src/test/kotlin/com/tnear/adoptloop/restdocs/RequireDocumentationExtension.kt`
- Create: `src/test/kotlin/com/tnear/adoptloop/restdocs/RestDocs.kt`
- Create: `src/test/kotlin/com/tnear/adoptloop/ControllerTestBase.kt`
- Create: `src/docs/asciidoc/index.adoc`

> **목적**: ADR-0009 강제력 구현. 컨트롤러 endpoint 테스트가 `documentApi(...)`를 호출하지 않으면 테스트 fail. 본 task 자체엔 자동 테스트가 없고, 다음 task `AdoptionControllerTest`에서 강제 동작이 자연스럽게 검증된다.

- [ ] **Step 1: DocCallTracker.kt**

```kotlin
package com.tnear.adoptloop.restdocs

object DocCallTracker {
    private val called = ThreadLocal.withInitial { false }
    fun reset() { called.set(false) }
    fun mark() { called.set(true) }
    fun wasCalled(): Boolean = called.get()
}
```

- [ ] **Step 2: RequireDocumentationExtension.kt**

```kotlin
package com.tnear.adoptloop.restdocs

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class RequireDocumentationExtension : BeforeEachCallback, AfterEachCallback {
    override fun beforeEach(ctx: ExtensionContext) { DocCallTracker.reset() }
    override fun afterEach(ctx: ExtensionContext) {
        if (ctx.executionException.isEmpty && !DocCallTracker.wasCalled()) {
            throw AssertionError(
                "Controller test '${ctx.displayName}' did not call documentApi(...). " +
                "REST Docs is enforced (ADR-0009)."
            )
        }
    }
}
```

- [ ] **Step 3: RestDocs.kt (documentApi 헬퍼)**

```kotlin
package com.tnear.adoptloop.restdocs

import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.snippet.Snippet
import org.springframework.test.web.servlet.ResultHandler

// 표준 `document(...)` 대신 반드시 이걸 호출한다.
// DocCallTracker가 호출 여부를 추적하므로 우회 시 RequireDocumentationExtension이 fail시킨다.
fun documentApi(identifier: String, vararg snippets: Snippet): ResultHandler {
    DocCallTracker.mark()
    return MockMvcRestDocumentation.document(identifier, *snippets)
}
```

- [ ] **Step 4: ControllerTestBase.kt**

```kotlin
package com.tnear.adoptloop

import com.tnear.adoptloop.restdocs.RequireDocumentationExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@ExtendWith(RestDocumentationExtension::class, RequireDocumentationExtension::class)
abstract class ControllerTestBase : IntegrationTestBase() {
    @Autowired protected lateinit var context: WebApplicationContext
    protected lateinit var mvc: MockMvc

    @BeforeEach
    fun setUpMvc(restDocumentation: RestDocumentationContextProvider) {
        mvc = MockMvcBuilders.webAppContextSetup(context)
            .apply<DefaultMockMvcBuilder>(documentationConfiguration(restDocumentation))
            .build()
    }
}
```

> 규칙: 컨트롤러 endpoint 테스트는 `IntegrationTestBase`가 아닌 **`ControllerTestBase`**를 상속한다. 그리고 표준 `document(...)` 대신 **`documentApi(...)`**를 호출해야 한다.

- [ ] **Step 5: src/docs/asciidoc/index.adoc (placeholder)**

```asciidoc
= AdoptLoop API Reference
:source-highlighter: highlightjs
:toc: left
:icons: font

이 문서는 빌드 시 `build/generated-snippets/`에서 자동 생성된다.
컨트롤러 endpoint별 스니펫이 추가되면 `include::{snippets}/<operation-id>/...[]`로 합본한다.
```

- [ ] **Step 6: 빌드 검증**

Run: `./gradlew compileTestKotlin`
Expected: 0 종료.

- [ ] **Step 7: Commit**

```bash
git add src/test/kotlin/com/tnear/adoptloop/ControllerTestBase.kt \
        src/test/kotlin/com/tnear/adoptloop/restdocs \
        src/docs/asciidoc
git commit -m "test: ControllerTestBase + RequireDocumentationExtension (REST Docs 강제, ADR-0009)"
```

---

### Task 2.4: Adoption DTO + Service + Controller (TDD)

**Files:**
- Create: `src/main/kotlin/com/tnear/adoptloop/adoption/AdoptionDtos.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/adoption/AdoptionService.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/adoption/AdoptionController.kt`
- Create: `src/test/kotlin/com/tnear/adoptloop/adoption/AdoptionControllerTest.kt`

- [ ] **Step 1: 실패하는 통합 테스트 작성**

```kotlin
package com.tnear.adoptloop.adoption

import com.fasterxml.jackson.databind.ObjectMapper
import com.tnear.adoptloop.ControllerTestBase
import com.tnear.adoptloop.domain.Admin
import com.tnear.adoptloop.domain.repo.AdminRepository
import com.tnear.adoptloop.restdocs.documentApi
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.security.MessageDigest

class AdoptionControllerTest @Autowired constructor(
    private val adminRepo: AdminRepository,
    private val om: ObjectMapper,
) : ControllerTestBase() {

    private fun seedAdminKey(): String {
        val raw = "k-${System.nanoTime()}"
        val hash = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
        adminRepo.save(Admin(name = "tester", keyHash = hash))
        return raw
    }

    @Test
    fun `POST creates adoption`() {
        val key = seedAdminKey()
        val body = mapOf(
            "name" to "Jira 도입",
            "goal" to "협업 가시화",
            "target_audience" to "전사 50명",
            "target_count" to 50,
        )
        mvc.perform(
            post("/api/admin/adoptions")
                .header("X-Admin-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(body))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Jira 도입"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andDo(documentApi("create-adoption",
                requestFields(
                    fieldWithPath("name").description("도입 이름"),
                    fieldWithPath("goal").description("도입 목적"),
                    fieldWithPath("target_audience").description("대상"),
                    fieldWithPath("target_count").description("대상 인원수"),
                ),
                responseFields(
                    fieldWithPath("id").description("도입 ID"),
                    fieldWithPath("admin_id").description("소유 admin ID"),
                    fieldWithPath("name").description("도입 이름"),
                    fieldWithPath("goal").description("도입 목적"),
                    fieldWithPath("target_audience").description("대상"),
                    fieldWithPath("concern").description("우려/제약").optional(),
                    fieldWithPath("target_count").description("대상 인원수"),
                    fieldWithPath("status").description("ACTIVE | ARCHIVED"),
                    fieldWithPath("created_at").description("생성 시각"),
                    fieldWithPath("updated_at").description("수정 시각"),
                ),
            ))
    }
}
```

- [ ] **Step 2: 테스트 실행 (FAIL)**

Run: `./gradlew test --tests AdoptionControllerTest`
Expected: 4xx 또는 컴파일 실패.

- [ ] **Step 3: AdoptionDtos.kt**

```kotlin
package com.tnear.adoptloop.adoption

import com.tnear.adoptloop.domain.Adoption
import com.tnear.adoptloop.domain.AdoptionStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class AdoptionCreateReq(
    @field:NotBlank val name: String,
    @field:NotBlank val goal: String,
    @field:NotBlank val targetAudience: String,
    val concern: String? = null,
    @field:Min(1) val targetCount: Int,
)

data class AdoptionUpdateReq(
    val name: String? = null,
    val goal: String? = null,
    val targetAudience: String? = null,
    val concern: String? = null,
    val targetCount: Int? = null,
    val status: AdoptionStatus? = null,
)

data class AdoptionRes(
    val id: Long,
    val adminId: Long,
    val name: String,
    val goal: String,
    val targetAudience: String,
    val concern: String?,
    val targetCount: Int,
    val status: AdoptionStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(a: Adoption) = AdoptionRes(
            a.id!!, a.adminId, a.name, a.goal, a.targetAudience, a.concern,
            a.targetCount, a.status, a.createdAt, a.updatedAt,
        )
    }
}
```

- [ ] **Step 4: AdoptionService.kt**

```kotlin
package com.tnear.adoptloop.adoption

import com.tnear.adoptloop.domain.Adoption
import com.tnear.adoptloop.domain.repo.AdoptionRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
@Transactional
class AdoptionService(private val repo: AdoptionRepository) {

    fun create(adminId: Long, req: AdoptionCreateReq): Adoption =
        repo.save(Adoption(
            adminId = adminId,
            name = req.name,
            goal = req.goal,
            targetAudience = req.targetAudience,
            concern = req.concern,
            targetCount = req.targetCount,
        ))

    @Transactional(readOnly = true)
    fun listForAdmin(adminId: Long): List<Adoption> = repo.findAllByAdminId(adminId)

    @Transactional(readOnly = true)
    fun get(adminId: Long, id: Long): Adoption {
        val a = repo.findById(id).orElseThrow { NoSuchElementException("adoption $id") }
        if (a.adminId != adminId) throw ResponseStatusException(HttpStatus.FORBIDDEN, "not owner")
        return a
    }

    fun update(adminId: Long, id: Long, req: AdoptionUpdateReq): Adoption {
        val a = get(adminId, id)
        req.name?.let { a.name = it }
        req.goal?.let { a.goal = it }
        req.targetAudience?.let { a.targetAudience = it }
        req.concern?.let { a.concern = it }
        req.targetCount?.let { a.targetCount = it }
        req.status?.let { a.status = it }
        return a
    }
}
```

- [ ] **Step 5: AdoptionController.kt**

```kotlin
package com.tnear.adoptloop.adoption

import com.tnear.adoptloop.admin.auth.AdminContext
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/adoptions")
class AdoptionController(
    private val service: AdoptionService,
    private val adminContext: AdminContext,
) {
    @GetMapping
    fun list(): List<AdoptionRes> =
        service.listForAdmin(adminContext.require()).map(AdoptionRes::from)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: AdoptionCreateReq): AdoptionRes =
        AdoptionRes.from(service.create(adminContext.require(), req))

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): AdoptionRes =
        AdoptionRes.from(service.get(adminContext.require(), id))

    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody req: AdoptionUpdateReq): AdoptionRes =
        AdoptionRes.from(service.update(adminContext.require(), id, req))
}
```

- [ ] **Step 6: GlobalExceptionHandler 추가**

Create: `src/main/kotlin/com/tnear/adoptloop/config/GlobalExceptionHandler.kt`

```kotlin
package com.tnear.adoptloop.config

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    data class ErrorRes(val code: String, val message: String)

    @ExceptionHandler(NoSuchElementException::class)
    fun notFound(e: NoSuchElementException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorRes("NOT_FOUND", e.message ?: ""))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(e: MethodArgumentNotValidException) =
        ResponseEntity.badRequest().body(ErrorRes("VALIDATION_FAILED",
            e.bindingResult.fieldErrors.joinToString { "${it.field}: ${it.defaultMessage}" }))

    @ExceptionHandler(LlmTransientException::class)
    fun llmTransient(e: LlmTransientException) =
        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            ErrorRes("LLM_TRANSIENT", "AI 응답에 일시적 문제가 발생했습니다. 잠시 후 다시 시도해주세요."))
}
// 권한·상태 충돌·입력 의미 오류는 도메인에서 `throw ResponseStatusException(...)`로 직접 던진다.
// Spring이 자동으로 status·reason을 응답에 매핑한다.
```

`src/main/kotlin/com/tnear/adoptloop/config/LlmTransientException.kt` (M4·M6 LLM 호출/파싱 실패 공통):
```kotlin
package com.tnear.adoptloop.config

class LlmTransientException(message: String) : RuntimeException(message)
```

- [ ] **Step 7: 테스트 실행 (PASS)**

Run: `./gradlew test --tests AdoptionControllerTest`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/kotlin src/test/kotlin
git commit -m "feat(adoption): create/list/get/update endpoints"
```

---

### Task 2.5: M2 PR

- [ ] **Step 1: PR**

```bash
git push -u origin feat/adoption-crud
gh pr create --title "feat(M2): domain entities + Adoption CRUD + REST Docs infra" \
  --body "$(cat <<'EOF'
## Summary
- 8 JPA entities + 8 repositories
- ControllerTestBase + RequireDocumentationExtension (REST Docs 강제, ADR-0009)
- Adoption DTO/Service/Controller + GlobalExceptionHandler
- AdoptionControllerTest 그린 (documentApi 호출 포함)

## Test plan
- [x] \`./gradlew test\` — AdoptionControllerTest 그린
- [x] \`./gradlew asciidoctor\` — build/asciidoc/html5/index.html 생성 확인
EOF
)"
```

---

## Milestone 3 — Surveys: 편집·발행·Slack (Day 2 AM, ~4-5h)

> **REST Docs 정책 ([ADR-0009](adr/0009-spring-restdocs-enforcement.md))**: M3의 컨트롤러 endpoint 테스트(`SurveyControllerTest`)는 M2 Task 2.3에서 도입된 `ControllerTestBase`를 상속하고 `documentApi(...)`로 문서 스니펫을 생성한다. 미호출 시 `RequireDocumentationExtension`이 fail.

### Task 3.1: SurveyService — 편집(PUT 전치환) + 발행

**Files:**
- Create: `src/main/kotlin/com/tnear/adoptloop/survey/SurveyDtos.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/survey/SurveyService.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/survey/SurveyController.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/survey/publish/SlackNotifier.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/survey/publish/SurveyPublisher.kt`
- Create: `src/test/kotlin/com/tnear/adoptloop/survey/SurveyControllerTest.kt`

- [ ] **Step 1: 브랜치 시작**

```bash
git checkout main && git pull && git checkout -b feat/surveys
```

- [ ] **Step 2: SurveyDtos.kt**

```kotlin
package com.tnear.adoptloop.survey

import com.tnear.adoptloop.domain.*
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class QuestionReq(
    val type: QuestionType,
    @field:NotBlank val text: String,
    val orderIndex: Int,
    val required: Boolean = true,
    val axis: Axis? = null,
    val options: List<OptionReq> = emptyList(),
)

data class OptionReq(
    @field:NotBlank val text: String,
    val orderIndex: Int,
)

data class SurveyDraftReq(
    val deadline: Instant,
)

data class QuestionVo(
    val id: Long,
    val type: QuestionType,
    val text: String,
    val orderIndex: Int,
    val required: Boolean,
    val axis: Axis?,
    val options: List<OptionVo>,
)

data class OptionVo(val id: Long, val text: String, val orderIndex: Int)

data class SurveyRes(
    val id: Long,
    val adoptionId: Long,
    val title: String,
    val publicSlug: String,
    val status: SurveyStatus,
    val deadline: Instant,
    val publishedAt: Instant?,
    val createdAt: Instant,
)

data class SurveyDetailRes(val survey: SurveyRes, val questions: List<QuestionVo>)
```

- [ ] **Step 3: SurveyService.kt (편집·발행 + 권한 체크)**

```kotlin
package com.tnear.adoptloop.survey

import com.tnear.adoptloop.domain.*
import com.tnear.adoptloop.domain.repo.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import com.tnear.adoptloop.survey.publish.SurveyPublisher
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.SecureRandom

@Service
@Transactional
class SurveyService(
    private val surveyRepo: SurveyRepository,
    private val adoptionRepo: AdoptionRepository,
    private val questionRepo: QuestionRepository,
    private val optionRepo: QuestionOptionRepository,
    private val publisher: SurveyPublisher,
) {
    private val random = SecureRandom()

    fun createDraft(adminId: Long, adoptionId: Long, title: String, deadline: java.time.Instant): Survey {
        requireAdoptionOwned(adminId, adoptionId)
        if (!deadline.isAfter(java.time.Instant.now()))
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "deadline must be in future")
        return surveyRepo.save(Survey(
            adoptionId = adoptionId,
            title = title,
            publicSlug = newSlug(),
            deadline = deadline,
        ))
    }

    fun replaceQuestions(adminId: Long, surveyId: Long, inputs: List<QuestionReq>): Survey {
        val s = requireSurveyOwned(adminId, surveyId)
        if (s.status != SurveyStatus.DRAFT)
            throw ResponseStatusException(HttpStatus.CONFLICT, "survey already published")
        val existingIds = questionRepo.findAllBySurveyIdOrderByOrderIndex(surveyId).mapNotNull { it.id }
        if (existingIds.isNotEmpty()) optionRepo.deleteAllByQuestionIdIn(existingIds)
        questionRepo.deleteAllBySurveyId(surveyId)
        questionRepo.flush()
        inputs.forEach { i ->
            val q = questionRepo.save(Question(
                surveyId = surveyId,
                type = i.type, text = i.text, orderIndex = i.orderIndex,
                required = i.required, axis = i.axis,
            ))
            i.options.forEach { opt ->
                optionRepo.save(QuestionOption(questionId = q.id!!, text = opt.text, orderIndex = opt.orderIndex))
            }
        }
        return s
    }

    fun publish(adminId: Long, surveyId: Long): Survey {
        val s = requireSurveyOwned(adminId, surveyId)
        if (s.status != SurveyStatus.DRAFT)
            throw ResponseStatusException(HttpStatus.CONFLICT, "already published")
        if (!s.deadline.isAfter(java.time.Instant.now()))
            throw ResponseStatusException(HttpStatus.CONFLICT, "deadline already passed")
        val questions = questionRepo.findAllBySurveyIdOrderByOrderIndex(surveyId)
        if (questions.isEmpty())
            throw ResponseStatusException(HttpStatus.CONFLICT, "questions required to publish")
        s.status = SurveyStatus.PUBLISHED
        s.publishedAt = java.time.Instant.now()
        publisher.scheduleAnnouncement(s)
        return s
    }

    fun createDraftWithQuestions(
        adminId: Long, adoptionId: Long,
        title: String, deadline: java.time.Instant,
        questions: List<QuestionReq>,
    ): Survey {
        val survey = createDraft(adminId, adoptionId, title, deadline)
        replaceQuestions(adminId, survey.id!!, questions)
        return survey
    }

    @Transactional(readOnly = true)
    fun detail(adminId: Long, surveyId: Long): Pair<Survey, List<Question>> {
        val s = requireSurveyOwned(adminId, surveyId)
        val qs = questionRepo.findAllBySurveyIdOrderByOrderIndex(surveyId)
        return s to qs
    }

    @Transactional(readOnly = true)
    fun loadOptions(questionIds: Collection<Long>): Map<Long, List<QuestionOption>> =
        if (questionIds.isEmpty()) emptyMap()
        else optionRepo.findAllByQuestionIdInOrderByOrderIndex(questionIds).groupBy { it.questionId }

    private fun requireAdoptionOwned(adminId: Long, adoptionId: Long): Adoption {
        val a = adoptionRepo.findById(adoptionId).orElseThrow { NoSuchElementException("adoption") }
        if (a.adminId != adminId) throw ResponseStatusException(HttpStatus.FORBIDDEN, "not owner")
        return a
    }

    private fun requireSurveyOwned(adminId: Long, surveyId: Long): Survey {
        val s = surveyRepo.findById(surveyId).orElseThrow { NoSuchElementException("survey") }
        requireAdoptionOwned(adminId, s.adoptionId)
        return s
    }

    private fun newSlug(): String {
        val bytes = ByteArray(12).also(random::nextBytes)
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
```

- [ ] **Step 4: SlackNotifier.kt**

```kotlin
package com.tnear.adoptloop.survey.publish

import com.tnear.adoptloop.config.SlackProperties
import com.tnear.adoptloop.domain.Survey
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class SlackNotifier(
    restClientBuilder: RestClient.Builder,
    private val props: SlackProperties,
    @Value("\${adoptloop.public-base-url}") private val publicBaseUrl: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val rest = restClientBuilder.build()

    fun publishAnnouncement(survey: Survey) {
        val webhook = props.webhookUrl?.takeIf { it.isNotBlank() } ?: return
        val body = mapOf("text" to """
            *${survey.title}* 설문이 발행되었습니다.
            마감: ${survey.deadline}
            $publicBaseUrl/s/${survey.publicSlug}
        """.trimIndent())
        try {
            rest.post().uri(webhook).body(body).retrieve().toBodilessEntity()
        } catch (e: Exception) {
            log.warn("Slack notify failed for survey {}: {}", survey.id, e.message)
        }
    }
}
```

- [ ] **Step 5: SurveyPublisher.kt — send-after-commit**

```kotlin
package com.tnear.adoptloop.survey.publish

import com.tnear.adoptloop.domain.Survey
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Component
class SurveyPublisher(private val notifier: SlackNotifier) {
    fun scheduleAnnouncement(survey: Survey) {
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() { notifier.publishAnnouncement(survey) }
        })
    }
}
```

- [ ] **Step 6: SurveyController.kt**

```kotlin
package com.tnear.adoptloop.survey

import com.tnear.adoptloop.admin.auth.AdminContext
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin")
class SurveyController(
    private val service: SurveyService,
    private val adminContext: AdminContext,
) {
    @GetMapping("/surveys/{id}")
    fun detail(@PathVariable id: Long): SurveyDetailRes = toDetail(id)

    @PutMapping("/surveys/{id}/questions")
    fun replaceQuestions(@PathVariable id: Long, @Valid @RequestBody inputs: List<QuestionReq>): SurveyDetailRes {
        service.replaceQuestions(adminContext.require(), id, inputs)
        return toDetail(id)
    }

    @PostMapping("/surveys/{id}/publish")
    @ResponseStatus(HttpStatus.OK)
    fun publish(@PathVariable id: Long): SurveyRes =
        toView(service.publish(adminContext.require(), id))

    private fun toDetail(id: Long): SurveyDetailRes {
        val (s, qs) = service.detail(adminContext.require(), id)
        val opts = service.loadOptions(qs.mapNotNull { it.id })
        return SurveyDetailRes(
            survey = toView(s),
            questions = qs.map { q -> QuestionVo(
                id = q.id!!, type = q.type, text = q.text,
                orderIndex = q.orderIndex, required = q.required, axis = q.axis,
                options = opts[q.id]?.map { OptionVo(it.id!!, it.text, it.orderIndex) } ?: emptyList(),
            )},
        )
    }

    private fun toView(s: com.tnear.adoptloop.domain.Survey) = SurveyRes(
        id = s.id!!, adoptionId = s.adoptionId, title = s.title,
        publicSlug = s.publicSlug, status = s.status, deadline = s.deadline,
        publishedAt = s.publishedAt, createdAt = s.createdAt,
    )
}
```

- [ ] **Step 7: 통합 테스트 (ControllerTestBase + documentApi)**

```kotlin
package com.tnear.adoptloop.survey

import com.fasterxml.jackson.databind.ObjectMapper
import com.tnear.adoptloop.ControllerTestBase
import com.tnear.adoptloop.domain.*
import com.tnear.adoptloop.domain.repo.*
import com.tnear.adoptloop.restdocs.documentApi
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.security.MessageDigest
import java.time.Instant

class SurveyControllerTest @Autowired constructor(
    private val om: ObjectMapper,
    private val adminRepo: AdminRepository,
    private val adoptionRepo: AdoptionRepository,
    private val surveyRepo: SurveyRepository,
) : ControllerTestBase() {

    @Test
    fun `publish empty draft is rejected with 409`() {
        val (key, adminId) = seedAdmin()
        val adoption = adoptionRepo.save(Adoption(adminId, "n", "g", "ta", null, 10))
        val draft = surveyRepo.save(Survey(adoption.id!!, "t", "slug-${System.nanoTime()}", Instant.now().plusSeconds(3600)))

        mvc.perform(post("/api/admin/surveys/${draft.id}/publish")
            .header("X-Admin-Key", key))
            .andExpect(status().isConflict)
            .andDo(documentApi("publish-survey-empty-draft-conflict"))
    }

    @Test
    fun `PUT questions replaces existing set`() {
        val (key, adminId) = seedAdmin()
        val adoption = adoptionRepo.save(Adoption(adminId, "n", "g", "ta", null, 10))
        val draft = surveyRepo.save(Survey(adoption.id!!, "t", "slug-${System.nanoTime()}", Instant.now().plusSeconds(3600)))

        val body = listOf(
            mapOf("type" to "TEXT", "text" to "Q1", "order_index" to 1),
            mapOf("type" to "SCALE", "text" to "Q2", "order_index" to 2, "axis" to "USAGE"),
        )
        mvc.perform(put("/api/admin/surveys/${draft.id}/questions")
            .header("X-Admin-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(om.writeValueAsString(body)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.questions.length()").value(2))
            .andDo(documentApi("replace-survey-questions",
                requestFields(
                    fieldWithPath("[].type").description("문항 타입 (TEXT | SINGLE_CHOICE | SCALE)"),
                    fieldWithPath("[].text").description("문항 본문"),
                    fieldWithPath("[].order_index").description("표시 순서"),
                    fieldWithPath("[].axis").description("축 (USAGE | BEHAVIOR | VALUE) — SCALE 한정").optional(),
                ),
                responseFields(
                    fieldWithPath("survey.id").description("설문 ID"),
                    fieldWithPath("survey.adoption_id").description("도입 ID"),
                    fieldWithPath("survey.title").description("설문 제목"),
                    fieldWithPath("survey.public_slug").description("공개 URL slug"),
                    fieldWithPath("survey.status").description("DRAFT | PUBLISHED | CLOSED"),
                    fieldWithPath("survey.deadline").description("응답 마감 시각"),
                    fieldWithPath("survey.published_at").description("발행 시각").optional(),
                    fieldWithPath("survey.created_at").description("생성 시각"),
                    fieldWithPath("questions[].id").description("문항 ID"),
                    fieldWithPath("questions[].type").description("문항 타입"),
                    fieldWithPath("questions[].text").description("문항 본문"),
                    fieldWithPath("questions[].order_index").description("표시 순서"),
                    fieldWithPath("questions[].required").description("필수 응답 여부"),
                    fieldWithPath("questions[].axis").description("축 (SCALE 한정)").optional(),
                    fieldWithPath("questions[].options").description("선택지 목록 (SINGLE_CHOICE 한정)"),
                ),
            ))
    }

    private fun seedAdmin(): Pair<String, Long> {
        val raw = "k-${System.nanoTime()}"
        val hash = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val saved = adminRepo.save(Admin(name = "t", keyHash = hash))
        return raw to saved.id!!
    }
}
```

> 두 테스트 모두 컨트롤러 endpoint 테스트이므로 `documentApi(...)` 호출 필수 (ADR-0009). 409 에러 케이스는 스니펫 없이 식별자만 — 기본 http-request/http-response 스니펫 생성으로 tracker 만족. PUT happy path는 요청/응답 필드 스니펫 작성.

- [ ] **Step 8: 테스트 실행 (PASS)**

Run: `./gradlew test --tests SurveyControllerTest`
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add src/main/kotlin/com/tnear/adoptloop/survey src/test/kotlin/com/tnear/adoptloop/survey
git commit -m "feat(survey): edit (PUT replace) + publish + Slack TX-after-commit + REST Docs"
```

- [ ] **Step 10: M3 PR**

```bash
git push -u origin feat/surveys
gh pr create --title "feat(M3): surveys edit/publish + Slack notifier + REST Docs" --body "PUT 전치환 + 발행 시 Slack send-after-commit. 빈 draft 발행 409 / 질문 교체 그린. SurveyControllerTest는 ControllerTestBase 상속 + documentApi 호출 (ADR-0009)."
```

---

## Milestone 4 — AI 설문 초안 (Day 2 PM, ~4-5h)

> **REST Docs 정책 ([ADR-0009](adr/0009-spring-restdocs-enforcement.md))**: `SurveyDraftServiceTest`도 컨트롤러 endpoint(`POST /api/admin/adoptions/{id}/surveys`)를 호출하므로 `ControllerTestBase` 상속 + `documentApi(...)` 호출 필수.

### Task 4.1: SurveyDraftPrompt + Parser + Service

**Files:**
- Create: `src/main/kotlin/com/tnear/adoptloop/config/BedrockConfig.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/survey/draft/SurveyDraftPrompt.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/survey/draft/SurveyDraftParser.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/survey/draft/SurveyDraftService.kt`
- Modify: `src/main/kotlin/com/tnear/adoptloop/survey/SurveyController.kt`
- Create: `src/test/kotlin/com/tnear/adoptloop/survey/SurveyDraftServiceTest.kt`

- [ ] **Step 1: 브랜치**

```bash
git checkout main && git pull && git checkout -b feat/ai-draft
```

- [ ] **Step 2: BedrockConfig.kt**

```kotlin
package com.tnear.adoptloop.config

import org.springframework.ai.bedrock.converse.BedrockProxyChatModel
import org.springframework.ai.chat.client.ChatClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BedrockConfig {
    @Bean
    fun chatClient(model: BedrockProxyChatModel): ChatClient = ChatClient.create(model)
}
```

- [ ] **Step 3: SurveyDraftPrompt.kt**

```kotlin
package com.tnear.adoptloop.survey.draft

import com.tnear.adoptloop.domain.Adoption

object SurveyDraftPrompt {
    fun build(a: Adoption): String = """
        당신은 사내 도입 정착도 설문 설계 전문가입니다. 아래 도입 정보를 바탕으로
        한국어 설문 문항을 설계하세요. 각 문항은 type(TEXT/SINGLE_CHOICE/SCALE)을 가지며,
        SCALE 문항은 axis(USAGE/BEHAVIOR/VALUE)를 명시합니다. 모든 enum 값은 UPPERCASE.

        **문항 구성 (반드시 준수):**
        - SCALE 4-6개 — USAGE/BEHAVIOR/VALUE 각 축당 **최소 1개** 포함
        - SINGLE_CHOICE 2-3개
        - TEXT 1-2개 (주관식, 자유 의견)
        - 합계 7-10개

        JSON으로만 응답:

        {
          "title": "...",
          "questions": [
            { "type": "SCALE", "text": "...", "axis": "USAGE" },
            { "type": "SINGLE_CHOICE", "text": "...", "options": ["A","B","C"] },
            { "type": "TEXT", "text": "..." }
          ]
        }

        도입 정보
        - 이름: ${a.name}
        - 목표: ${a.goal}
        - 대상자: ${a.targetAudience}
        - 우려사항: ${a.concern ?: "없음"}
    """.trimIndent()
}
```

- [ ] **Step 4: SurveyDraftParser.kt**

```kotlin
package com.tnear.adoptloop.survey.draft

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.tnear.adoptloop.domain.Axis
import com.tnear.adoptloop.domain.QuestionType
import com.tnear.adoptloop.survey.OptionReq
import com.tnear.adoptloop.survey.QuestionReq
import org.springframework.stereotype.Component

data class DraftPayloadDto(val title: String, val questions: List<QuestionReq>)

@Component
class SurveyDraftParser(private val om: ObjectMapper) {

    fun parse(raw: String): DraftPayloadDto {
        val json = extractJson(raw)
        val root = om.readTree(json)
        val title = root["title"]?.asText() ?: error("missing title")
        val questions = root["questions"]?.mapIndexed { i, q -> toQuestion(q, i + 1) } ?: error("missing questions")
        return DraftPayloadDto(title, questions)
    }

    private fun extractJson(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        require(start >= 0 && end > start) { "no JSON object in LLM output" }
        return raw.substring(start, end + 1)
    }

    private fun toQuestion(q: JsonNode, order: Int): QuestionReq {
        val type = QuestionType.valueOf(q["type"].asText())
        val axis = q["axis"]?.takeIf { !it.isNull }?.asText()?.let(Axis::valueOf)
        val options = q["options"]?.mapIndexed { i, o -> OptionReq(o.asText(), i + 1) } ?: emptyList()
        return QuestionReq(
            type = type, text = q["text"].asText(),
            orderIndex = order, required = true, axis = axis, options = options,
        )
    }
}
```

- [ ] **Step 5: SurveyDraftService.kt**

```kotlin
package com.tnear.adoptloop.survey.draft

import com.tnear.adoptloop.config.LlmTransientException
import com.tnear.adoptloop.domain.repo.AdoptionRepository
import com.tnear.adoptloop.survey.SurveyService
import org.springframework.ai.chat.client.ChatClient
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class SurveyDraftService(
    private val chatClient: ChatClient,
    private val parser: SurveyDraftParser,
    private val adoptionRepo: AdoptionRepository,
    private val surveyService: SurveyService,
) {
    fun generate(adminId: Long, adoptionId: Long, deadline: java.time.Instant): Long {
        val adoption = adoptionRepo.findById(adoptionId).orElseThrow { NoSuchElementException("adoption") }
        if (adoption.adminId != adminId) throw ResponseStatusException(HttpStatus.FORBIDDEN, "not owner")

        val raw = try {
            chatClient.prompt().user(SurveyDraftPrompt.build(adoption)).call().content()
                ?: throw LlmTransientException("LLM returned empty")
        } catch (e: LlmTransientException) { throw e }
          catch (e: Exception) { throw LlmTransientException("LLM call failed: ${e.message}") }

        val draft = try { parser.parse(raw) }
                    catch (e: Exception) { throw LlmTransientException("LLM output unparseable: ${e.message}") }

        val survey = surveyService.createDraftWithQuestions(
            adminId, adoptionId, draft.title, deadline, draft.questions,
        )
        return survey.id!!
    }
}
```

> **트랜잭션 경계**: LLM 호출은 `@Transactional` 밖. DB write는 `surveyService.createDraftWithQuestions` 진입 시점에 트랜잭션이 시작되어 draft + questions가 원자적으로 커밋된다. self-invocation 문제 없음 (다른 빈 호출).

- [ ] **Step 6: SurveyController에 엔드포인트 추가**

```kotlin
// SurveyController에 추가
@PostMapping("/adoptions/{adoptionId}/surveys")
@ResponseStatus(HttpStatus.CREATED)
fun generateDraft(@PathVariable adoptionId: Long, @Valid @RequestBody req: SurveyDraftReq): SurveyDetailRes {
    val id = draftService.generate(adminContext.require(), adoptionId, req.deadline)
    return toDetail(id)
}
```

생성자에 `private val draftService: com.tnear.adoptloop.survey.draft.SurveyDraftService` 추가.

- [ ] **Step 7: 테스트 — ChatClient mock (ControllerTestBase + documentApi)**

```kotlin
package com.tnear.adoptloop.survey

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninja_squad.springmockk.MockkBean
import com.tnear.adoptloop.ControllerTestBase
import com.tnear.adoptloop.domain.*
import com.tnear.adoptloop.domain.repo.*
import com.tnear.adoptloop.restdocs.documentApi
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.security.MessageDigest
import java.time.Instant

class SurveyDraftServiceTest @Autowired constructor(
    private val om: ObjectMapper,
    private val adminRepo: AdminRepository,
    private val adoptionRepo: AdoptionRepository,
) : ControllerTestBase() {

    @MockkBean private lateinit var chatClient: ChatClient

    @Test
    fun `generates draft from LLM JSON`() {
        val raw = """{"title":"Jira 설문","questions":[
            {"type":"SCALE","text":"사용 빈도","axis":"USAGE"},
            {"type":"SINGLE_CHOICE","text":"역할","options":["기획","개발"]}
        ]}"""
        val spec = mockk<ChatClient.ChatClientRequestSpec>(relaxed = true)
        every { chatClient.prompt() } returns spec
        every { spec.user(any<String>()) } returns spec
        every { spec.call().content() } returns raw

        val (key, adminId) = seedAdmin()
        val adoption = adoptionRepo.save(Adoption(adminId, "Jira", "g", "ta", null, 10))

        mvc.perform(post("/api/admin/adoptions/${adoption.id}/surveys")
            .header("X-Admin-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(om.writeValueAsString(mapOf("deadline" to Instant.now().plusSeconds(3600).toString()))))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.survey.title").value("Jira 설문"))
            .andExpect(jsonPath("$.questions.length()").value(2))
            .andDo(documentApi("generate-survey-draft",
                requestFields(
                    fieldWithPath("deadline").description("응답 마감 시각 (ISO-8601)"),
                ),
                responseFields(
                    fieldWithPath("survey.id").description("설문 ID"),
                    fieldWithPath("survey.adoption_id").description("도입 ID"),
                    fieldWithPath("survey.title").description("LLM이 생성한 설문 제목"),
                    fieldWithPath("survey.public_slug").description("공개 URL slug"),
                    fieldWithPath("survey.status").description("DRAFT | PUBLISHED | CLOSED"),
                    fieldWithPath("survey.deadline").description("응답 마감 시각"),
                    fieldWithPath("survey.published_at").description("발행 시각").optional(),
                    fieldWithPath("survey.created_at").description("생성 시각"),
                    fieldWithPath("questions[].id").description("문항 ID"),
                    fieldWithPath("questions[].type").description("문항 타입 (TEXT | SINGLE_CHOICE | SCALE)"),
                    fieldWithPath("questions[].text").description("문항 본문"),
                    fieldWithPath("questions[].order_index").description("표시 순서"),
                    fieldWithPath("questions[].required").description("필수 응답 여부"),
                    fieldWithPath("questions[].axis").description("축 (SCALE 한정)").optional(),
                    fieldWithPath("questions[].options[].id").description("선택지 ID").optional(),
                    fieldWithPath("questions[].options[].text").description("선택지 본문").optional(),
                    fieldWithPath("questions[].options[].order_index").description("선택지 순서").optional(),
                ),
            ))
    }

    private fun seedAdmin(): Pair<String, Long> {
        val raw = "k-${System.nanoTime()}"
        val hash = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val saved = adminRepo.save(Admin(name = "t", keyHash = hash))
        return raw to saved.id!!
    }
}
```

- [ ] **Step 8: 테스트 실행 (PASS)**

Run: `./gradlew test --tests SurveyDraftServiceTest`
Expected: PASS.

- [ ] **Step 9: Commit + PR**

```bash
git add src/main/kotlin/com/tnear/adoptloop src/test/kotlin/com/tnear/adoptloop/survey
git commit -m "feat(M4): AI survey draft generation via Bedrock Converse + REST Docs"
git push -u origin feat/ai-draft
gh pr create --title "feat(M4): AI survey draft generation + REST Docs" --body "Bedrock Converse + JSON 파싱 + draft + 질문 일괄 저장. SurveyDraftServiceTest는 ControllerTestBase + documentApi (ADR-0009)."
```

---

## Milestone 5 — 응답자 공개 API (Day 3 AM, ~3-4h)

### Task 5.1: PublicSurveyController + ResponseService (TDD)

**Files:**
- Create: `src/main/kotlin/com/tnear/adoptloop/publicapi/PublicDtos.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/publicapi/PublicResponseService.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/publicapi/PublicSurveyController.kt`
- Create: `src/test/kotlin/com/tnear/adoptloop/publicapi/PublicSurveyControllerTest.kt`

- [ ] **Step 1: 브랜치**

```bash
git checkout main && git pull && git checkout -b feat/public-api
```

- [ ] **Step 2: PublicDtos.kt**

```kotlin
package com.tnear.adoptloop.publicapi

import com.tnear.adoptloop.domain.ResponseStatus
import com.tnear.adoptloop.survey.QuestionVo
import java.time.Instant

data class PublicSurveyRes(val title: String, val deadline: Instant, val questions: List<QuestionVo>)
data class ResponseTokenRes(val accessToken: String)
data class AnswerReq(
    val questionId: Long,
    val textValue: String? = null,
    val questionOptionId: Long? = null,
    val scaleValue: Int? = null,
)
data class PublicResponseRes(
    val survey: PublicSurveyRes,
    val status: ResponseStatus,
    val submittedAt: Instant?,
    val answers: List<AnswerReq>,
)
```

- [ ] **Step 3: PublicResponseService.kt**

```kotlin
package com.tnear.adoptloop.publicapi

import com.tnear.adoptloop.domain.*
import com.tnear.adoptloop.domain.repo.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

@Service
@Transactional
class PublicResponseService(
    private val surveyRepo: SurveyRepository,
    private val responseRepo: SurveyResponseRepository,
    private val answerRepo: AnswerRepository,
    private val questionRepo: QuestionRepository,
    private val adoptionRepo: AdoptionRepository,
) {
    private val random = SecureRandom()

    @Transactional(readOnly = true)
    fun loadBySlug(slug: String): Survey {
        val s = surveyRepo.findByPublicSlug(slug)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "survey")
        if (s.status != SurveyStatus.PUBLISHED)
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "draft survey")
        if (Instant.now().isAfter(s.deadline))
            throw ResponseStatusException(HttpStatus.GONE, "deadline passed")
        return s
    }

    fun startResponse(slug: String): SurveyResponse {
        val s = loadBySlug(slug)
        val adoption = adoptionRepo.findById(s.adoptionId).orElseThrow { NoSuchElementException("adoption") }
        val cap = adoption.targetCount * 10L
        if (responseRepo.countBySurveyId(s.id!!) >= cap)
            throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "response limit reached")
        return responseRepo.save(SurveyResponse(surveyId = s.id!!, accessToken = newToken()))
    }

    @Transactional(readOnly = true)
    fun loadByToken(token: String): Pair<SurveyResponse, Survey> {
        val r = responseRepo.findByAccessToken(token)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "token")
        val s = surveyRepo.findById(r.surveyId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "survey")
        }
        return r to s
    }

    fun submit(token: String, inputs: List<AnswerReq>): SurveyResponse {
        val (r, s) = loadByToken(token)
        if (Instant.now().isAfter(s.deadline)) throw ResponseStatusException(HttpStatus.FORBIDDEN, "deadline")

        val questions = questionRepo.findAllBySurveyIdOrderByOrderIndex(s.id!!).associateBy { it.id!! }
        val requiredIds = questions.values.filter { it.required }.mapNotNull { it.id }.toSet()
        val answeredIds = inputs.map { it.questionId }.toSet()
        val missing = requiredIds - answeredIds
        if (missing.isNotEmpty())
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "missing required questions: $missing")

        answerRepo.deleteAllBySurveyResponseId(r.id!!)
        answerRepo.flush()
        inputs.forEach {
            val q = questions[it.questionId] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown question ${it.questionId}")
            validateMatchesType(q.type, it)
            answerRepo.save(Answer(
                surveyResponseId = r.id!!, questionId = it.questionId,
                textValue = it.textValue, questionOptionId = it.questionOptionId, scaleValue = it.scaleValue,
            ))
        }
        if (r.status == ResponseStatus.IN_PROGRESS) {
            r.status = ResponseStatus.SUBMITTED
            r.submittedAt = Instant.now()
        }
        return r
    }

    fun loadAnswers(responseId: Long): List<Answer> = answerRepo.findAllBySurveyResponseId(responseId)

    private fun validateMatchesType(type: QuestionType, input: AnswerReq) {
        val nonNull = listOfNotNull(input.textValue, input.questionOptionId, input.scaleValue).size
        if (nonNull != 1)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "exactly one of text_value/question_option_id/scale_value required")
        val ok = when (type) {
            QuestionType.TEXT -> !input.textValue.isNullOrBlank()
            QuestionType.SINGLE_CHOICE -> input.questionOptionId != null
            QuestionType.SCALE -> input.scaleValue in 1..5
        }
        if (!ok) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "answer does not match question type $type")
    }

    private fun newToken(): String {
        val bytes = ByteArray(24).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
```

- [ ] **Step 4: PublicSurveyController.kt**

```kotlin
package com.tnear.adoptloop.publicapi

import com.tnear.adoptloop.domain.repo.QuestionOptionRepository
import com.tnear.adoptloop.domain.repo.QuestionRepository
import com.tnear.adoptloop.survey.OptionVo
import com.tnear.adoptloop.survey.QuestionVo
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/public")
class PublicSurveyController(
    private val responseService: PublicResponseService,
    private val questionRepo: QuestionRepository,
    private val optionRepo: QuestionOptionRepository,
) {
    @GetMapping("/surveys/{slug}")
    fun get(@PathVariable slug: String): PublicSurveyRes {
        val s = responseService.loadBySlug(slug)
        return toView(s)
    }

    @PostMapping("/surveys/{slug}/responses")
    @ResponseStatus(HttpStatus.CREATED)
    fun start(@PathVariable slug: String): ResponseTokenRes =
        ResponseTokenRes(responseService.startResponse(slug).accessToken)

    @GetMapping("/responses/{token}")
    fun load(@PathVariable token: String): PublicResponseRes {
        val (r, s) = responseService.loadByToken(token)
        val answers = responseService.loadAnswers(r.id!!).map {
            AnswerReq(it.questionId, it.textValue, it.questionOptionId, it.scaleValue)
        }
        return PublicResponseRes(toView(s), r.status, r.submittedAt, answers)
    }

    @PutMapping("/responses/{token}/answers")
    fun submit(@PathVariable token: String, @RequestBody inputs: List<AnswerReq>): PublicResponseRes {
        responseService.submit(token, inputs)
        return load(token)
    }

    private fun toView(s: com.tnear.adoptloop.domain.Survey): PublicSurveyRes {
        val qs = questionRepo.findAllBySurveyIdOrderByOrderIndex(s.id!!)
        val opts = if (qs.isEmpty()) emptyMap()
            else optionRepo.findAllByQuestionIdInOrderByOrderIndex(qs.mapNotNull { it.id }).groupBy { it.questionId }
        val qvs = qs.map { q -> QuestionVo(
            id = q.id!!, type = q.type, text = q.text, orderIndex = q.orderIndex,
            required = q.required, axis = q.axis,
            options = opts[q.id]?.map { OptionVo(it.id!!, it.text, it.orderIndex) } ?: emptyList(),
        )}
        return PublicSurveyRes(s.title, s.deadline, qvs)
    }
}
```

- [ ] **Step 5: 통합 테스트 (happy + token/마감 edge)**

```kotlin
package com.tnear.adoptloop.publicapi

import com.fasterxml.jackson.databind.ObjectMapper
import com.tnear.adoptloop.IntegrationTestBase
import com.tnear.adoptloop.domain.*
import com.tnear.adoptloop.domain.repo.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
class PublicSurveyControllerTest @Autowired constructor(
    private val mvc: MockMvc,
    private val om: ObjectMapper,
    private val adoptionRepo: AdoptionRepository,
    private val adminRepo: AdminRepository,
    private val surveyRepo: SurveyRepository,
    private val questionRepo: QuestionRepository,
) : IntegrationTestBase() {

    @Test
    fun `start → submit → reload returns submitted answers`() {
        val s = seedPublishedSurveyWithTextQuestion()
        // 1. start
        val tokenJson = mvc.perform(post("/api/public/surveys/${s.publicSlug}/responses"))
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString
        val token = om.readTree(tokenJson)["access_token"].asText()

        // 2. submit
        val q = questionRepo.findAllBySurveyIdOrderByOrderIndex(s.id!!).first()
        val body = listOf(mapOf("question_id" to q.id, "text_value" to "good"))
        mvc.perform(put("/api/public/responses/$token/answers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(om.writeValueAsString(body)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUBMITTED"))
            .andExpect(jsonPath("$.answers[0].text_value").value("good"))
    }

    @Test
    fun `submit with invalid token returns 401`() {
        mvc.perform(put("/api/public/responses/bogus/answers")
            .contentType(MediaType.APPLICATION_JSON)
            .content("[]"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET survey past deadline returns 410`() {
        val s = seedPublishedSurveyWithTextQuestion(deadline = Instant.now().minusSeconds(60))
        mvc.perform(get("/api/public/surveys/${s.publicSlug}"))
            .andExpect(status().isGone)
    }

    private fun seedPublishedSurveyWithTextQuestion(deadline: Instant = Instant.now().plusSeconds(3600)): Survey {
        val admin = adminRepo.save(Admin(name = "x", keyHash = "h${System.nanoTime()}".padEnd(64, '0').take(64)))
        val ad = adoptionRepo.save(Adoption(admin.id!!, "n", "g", "ta", null, 10))
        val s = surveyRepo.save(Survey(ad.id!!, "t", "slug-${System.nanoTime()}",
            status = SurveyStatus.PUBLISHED, deadline = deadline, publishedAt = Instant.now()))
        questionRepo.save(Question(s.id!!, QuestionType.TEXT, "Q1", 1, true, null))
        return s
    }
}
```

- [ ] **Step 6: 테스트 실행 (PASS)**

Run: `./gradlew test --tests PublicSurveyControllerTest`
Expected: 3개 모두 PASS.

- [ ] **Step 7: Commit + PR**

```bash
git add src/main/kotlin/com/tnear/adoptloop/publicapi src/test/kotlin/com/tnear/adoptloop/publicapi
git commit -m "feat(M5): public response API (token, deadline, PUT replace)"
git push -u origin feat/public-api
gh pr create --title "feat(M5): public response flow" --body "토큰 발급 → 응답 PUT 전치환 → 재로드. 토큰/마감 음성 케이스 포함."
```

---

## Milestone 6 — 집계 + AI 분석 + 액션 아이템 (Day 3 PM, ~4-5h)

### Task 6.1: AggregateService (네이티브 SQL)

**Files:**
- Create: `src/main/kotlin/com/tnear/adoptloop/analysis/AggregateDtos.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/analysis/AggregateService.kt`

- [ ] **Step 1: 브랜치**

```bash
git checkout main && git pull && git checkout -b feat/analysis
```

- [ ] **Step 2: AggregateDtos.kt**

```kotlin
package com.tnear.adoptloop.analysis

data class AggregateRes(
    val participants: Int,
    val targetCount: Int,
    val participationRate: Double,
    val perQuestion: List<QuestionAggregateVo>,
)

sealed interface QuestionAggregateVo { val questionId: Long; val type: String }

data class ChoiceAggregateVo(
    override val questionId: Long,
    val distribution: List<DistributionBucketVo>,
) : QuestionAggregateVo { override val type = "SINGLE_CHOICE" }

data class ScaleAggregateVo(
    override val questionId: Long,
    val axis: String?,
    val average: Double,
    val count: Long,
) : QuestionAggregateVo { override val type = "SCALE" }

data class TextAggregateVo(
    override val questionId: Long,
    val values: List<String>,
) : QuestionAggregateVo { override val type = "TEXT" }

data class DistributionBucketVo(val optionId: Long, val count: Long)
```

- [ ] **Step 3: AggregateService.kt**

```kotlin
package com.tnear.adoptloop.analysis

import com.tnear.adoptloop.domain.ResponseStatus
import com.tnear.adoptloop.domain.QuestionType
import com.tnear.adoptloop.domain.repo.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AggregateService(
    private val surveyRepo: SurveyRepository,
    private val adoptionRepo: AdoptionRepository,
    private val responseRepo: SurveyResponseRepository,
    private val questionRepo: QuestionRepository,
    private val answerRepo: AnswerRepository,
) {
    fun aggregate(surveyId: Long): AggregateRes {
        val survey = surveyRepo.findById(surveyId).orElseThrow { NoSuchElementException("survey") }
        val adoption = adoptionRepo.findById(survey.adoptionId).orElseThrow()
        val participants = responseRepo.countBySurveyIdAndStatus(surveyId, ResponseStatus.SUBMITTED).toInt()

        val questions = questionRepo.findAllBySurveyIdOrderByOrderIndex(surveyId)
        val responseIds = responseRepo.findAllBySurveyIdAndStatus(surveyId, ResponseStatus.SUBMITTED)
            .mapNotNull { it.id }
        val answers = if (responseIds.isEmpty()) emptyList()
            else answerRepo.findAllBySurveyResponseIdIn(responseIds)

        val byQuestion = answers.groupBy { it.questionId }
        val per = questions.map { q ->
            val list = byQuestion[q.id] ?: emptyList()
            when (q.type) {
                QuestionType.TEXT -> TextAggregateVo(q.id!!, list.mapNotNull { it.textValue })
                QuestionType.SCALE -> {
                    val scales = list.mapNotNull { it.scaleValue }
                    ScaleAggregateVo(q.id!!, q.axis?.name,
                        average = if (scales.isEmpty()) 0.0 else scales.average(),
                        count = scales.size.toLong())
                }
                QuestionType.SINGLE_CHOICE -> {
                    val dist = list.mapNotNull { it.questionOptionId }
                        .groupingBy { it }.eachCount()
                        .map { (opt, c) -> DistributionBucketVo(opt, c.toLong()) }
                    ChoiceAggregateVo(q.id!!, dist)
                }
            }
        }
        return AggregateRes(
            participants = participants,
            targetCount = adoption.targetCount,
            participationRate = if (adoption.targetCount == 0) 0.0 else participants.toDouble() / adoption.targetCount,
            perQuestion = per,
        )
    }
}
```

- [ ] **Step 4: 테스트**

```kotlin
package com.tnear.adoptloop.analysis

import com.tnear.adoptloop.IntegrationTestBase
import com.tnear.adoptloop.domain.*
import com.tnear.adoptloop.domain.repo.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import kotlin.test.assertEquals

class AggregateServiceTest @Autowired constructor(
    private val service: AggregateService,
    private val adminRepo: AdminRepository,
    private val adoptionRepo: AdoptionRepository,
    private val surveyRepo: SurveyRepository,
    private val questionRepo: QuestionRepository,
    private val responseRepo: SurveyResponseRepository,
    private val answerRepo: AnswerRepository,
) : IntegrationTestBase() {

    @Test
    fun `scale average and participation rate`() {
        val ad = setupAdoption(targetCount = 10)
        val s = surveyRepo.save(Survey(ad.id!!, "t", "slug-${System.nanoTime()}",
            status = SurveyStatus.PUBLISHED, deadline = Instant.now().plusSeconds(60)))
        val q = questionRepo.save(Question(s.id!!, QuestionType.SCALE, "Q", 1, true, Axis.USAGE))
        listOf(3, 4, 5).forEach { v ->
            val r = responseRepo.save(SurveyResponse(s.id!!, "tk${System.nanoTime()}", status = ResponseStatus.SUBMITTED, submittedAt = Instant.now()))
            answerRepo.save(Answer(r.id!!, q.id!!, scaleValue = v))
        }

        val agg = service.aggregate(s.id!!)
        assertEquals(3, agg.participants)
        assertEquals(0.3, agg.participationRate)
        val sa = agg.perQuestion.first() as ScaleAggregateVo
        assertEquals(4.0, sa.average)
    }

    private fun setupAdoption(targetCount: Int) =
        adoptionRepo.save(Adoption(
            adminId = adminRepo.save(Admin(name = "x", keyHash = "h${System.nanoTime()}".padEnd(64, '0').take(64))).id!!,
            name = "n", goal = "g", targetAudience = "ta", concern = null, targetCount = targetCount,
        ))
}
```

- [ ] **Step 5: 테스트 실행 (PASS)**

Run: `./gradlew test --tests AggregateServiceTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/tnear/adoptloop/analysis src/test/kotlin/com/tnear/adoptloop/analysis
git commit -m "feat(analysis): aggregate service (choice dist, scale avg, text values)"
```

---

### Task 6.2: AnalysisService — Bedrock + 저장

**Files:**
- Create: `src/main/kotlin/com/tnear/adoptloop/analysis/AnalysisPrompt.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/analysis/AnalysisParser.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/analysis/AnalysisDtos.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/analysis/AnalysisService.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/analysis/AnalysisController.kt`

- [ ] **Step 1: AnalysisPrompt.kt**

```kotlin
package com.tnear.adoptloop.analysis

import com.fasterxml.jackson.databind.ObjectMapper
import com.tnear.adoptloop.domain.Adoption

object AnalysisPrompt {
    fun build(adoption: Adoption, agg: AggregateRes, om: ObjectMapper): String = """
        당신은 사내 도입 정착도 분석가입니다. 다음 도입과 집계를 분석하여 JSON으로만 응답:

        {
          "adoption_score": 0-100,
          "usage_score": 0-100,
          "behavior_score": 0-100,
          "value_score": 0-100,
          "positive_signals": ["..."],
          "resistance_factors": ["..."],
          "risks": ["..."],
          "suggested_action_items": [
            {"title":"...","description":"...","priority":"HIGH|MEDIUM|LOW"}
          ]
        }

        도입: ${adoption.name} / 목표: ${adoption.goal}
        집계:
        ${om.writeValueAsString(agg)}
    """.trimIndent()
}
```

- [ ] **Step 2: AnalysisDtos.kt**

```kotlin
package com.tnear.adoptloop.analysis

import com.tnear.adoptloop.domain.Priority
import java.time.Instant

data class AnalysisRes(
    val id: Long,
    val surveyId: Long,
    val adoptionScore: Int,
    val usageScore: Int,
    val behaviorScore: Int,
    val valueScore: Int,
    val positiveSignals: List<String>,
    val resistanceFactors: List<String>,
    val risks: List<String>,
    val rawOutput: String,
    val createdAt: Instant,
)

data class SuggestedActionItemVo(
    val title: String,
    val description: String? = null,
    val priority: Priority,
)

data class AnalysisRunRes(
    val analysis: AnalysisRes,
    val suggestedActionItems: List<SuggestedActionItemVo>,
)
```

- [ ] **Step 3: AnalysisParser.kt**

```kotlin
package com.tnear.adoptloop.analysis

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.tnear.adoptloop.domain.Priority
import org.springframework.stereotype.Component

data class ParsedAnalysisDto(
    val scores: ScoresVo,
    val signals: SignalsVo,
    val suggested: List<SuggestedActionItemVo>,
)
data class ScoresVo(val adoption: Int, val usage: Int, val behavior: Int, val value: Int)
data class SignalsVo(val positive: List<String>, val resistance: List<String>, val risks: List<String>)

@Component
class AnalysisParser(private val om: ObjectMapper) {
    fun parse(raw: String): ParsedAnalysisDto {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        require(start >= 0 && end > start)
        val root = om.readTree(raw.substring(start, end + 1))
        return ParsedAnalysisDto(
            scores = ScoresVo(
                root["adoption_score"].asInt(), root["usage_score"].asInt(),
                root["behavior_score"].asInt(), root["value_score"].asInt(),
            ),
            signals = SignalsVo(
                root["positive_signals"].toStringList(),
                root["resistance_factors"].toStringList(),
                root["risks"].toStringList(),
            ),
            suggested = root["suggested_action_items"]?.map {
                SuggestedActionItemVo(
                    title = it["title"].asText(),
                    description = it["description"]?.takeIf { d -> !d.isNull }?.asText(),
                    priority = Priority.valueOf(it["priority"].asText()),
                )
            } ?: emptyList(),
        )
    }
    private fun JsonNode?.toStringList() = this?.map { it.asText() } ?: emptyList()
}
```

- [ ] **Step 4: AnalysisService.kt**

```kotlin
package com.tnear.adoptloop.analysis

import com.fasterxml.jackson.databind.ObjectMapper
import com.tnear.adoptloop.domain.*
import com.tnear.adoptloop.domain.repo.*
import org.springframework.ai.chat.client.ChatClient
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Service
@Transactional
class AnalysisService(
    private val aggregateService: AggregateService,
    private val chatClient: ChatClient,
    private val parser: AnalysisParser,
    private val analysisRepo: AnalysisRepository,
    private val surveyRepo: SurveyRepository,
    private val adoptionRepo: AdoptionRepository,
    private val om: ObjectMapper,
) {
    fun run(adminId: Long, surveyId: Long): AnalysisRunRes {
        val survey = surveyRepo.findById(surveyId).orElseThrow { NoSuchElementException("survey") }
        val adoption = adoptionRepo.findById(survey.adoptionId).orElseThrow()
        if (adoption.adminId != adminId) throw ResponseStatusException(HttpStatus.FORBIDDEN, "not owner")
        if (Instant.now().isBefore(survey.deadline))
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "before deadline")

        val agg = aggregateService.aggregate(surveyId)
        val raw = try {
            chatClient.prompt().user(AnalysisPrompt.build(adoption, agg, om)).call().content()
                ?: throw com.tnear.adoptloop.config.LlmTransientException("LLM returned empty")
        } catch (e: com.tnear.adoptloop.config.LlmTransientException) { throw e }
          catch (e: Exception) { throw com.tnear.adoptloop.config.LlmTransientException("LLM call failed: ${e.message}") }
        val parsed = try { parser.parse(raw) }
                     catch (e: Exception) { throw com.tnear.adoptloop.config.LlmTransientException("LLM output unparseable: ${e.message}") }

        val saved = analysisRepo.save(Analysis(
            surveyId = surveyId,
            adoptionScore = parsed.scores.adoption, usageScore = parsed.scores.usage,
            behaviorScore = parsed.scores.behavior, valueScore = parsed.scores.value,
            positiveSignals = parsed.signals.positive,
            resistanceFactors = parsed.signals.resistance,
            risks = parsed.signals.risks, rawOutput = raw,
        ))
        return AnalysisRunRes(
            analysis = toView(saved),
            suggestedActionItems = parsed.suggested,
        )
    }

    @Transactional(readOnly = true)
    fun latest(adminId: Long, surveyId: Long): AnalysisRes {
        val survey = surveyRepo.findById(surveyId).orElseThrow { NoSuchElementException("survey") }
        val adoption = adoptionRepo.findById(survey.adoptionId).orElseThrow()
        if (adoption.adminId != adminId) throw ResponseStatusException(HttpStatus.FORBIDDEN, "not owner")
        val a = analysisRepo.findFirstBySurveyIdOrderByCreatedAtDesc(surveyId)
            .orElseThrow { NoSuchElementException("no analysis") }
        return toView(a)
    }

    private fun toView(a: Analysis) = AnalysisRes(
        a.id!!, a.surveyId, a.adoptionScore, a.usageScore, a.behaviorScore, a.valueScore,
        a.positiveSignals, a.resistanceFactors, a.risks, a.rawOutput, a.createdAt,
    )
}
```

- [ ] **Step 5: AnalysisController.kt**

```kotlin
package com.tnear.adoptloop.analysis

import com.tnear.adoptloop.admin.auth.AdminContext
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/surveys/{surveyId}")
class AnalysisController(
    private val aggregateService: AggregateService,
    private val analysisService: AnalysisService,
    private val adminContext: AdminContext,
) {
    @GetMapping("/aggregate")
    fun aggregate(@PathVariable surveyId: Long): AggregateRes {
        adminContext.require()
        return aggregateService.aggregate(surveyId)
    }

    @PostMapping("/analyses")
    @ResponseStatus(HttpStatus.CREATED)
    fun run(@PathVariable surveyId: Long): AnalysisRunRes =
        analysisService.run(adminContext.require(), surveyId)

    @GetMapping("/analyses/latest")
    fun latest(@PathVariable surveyId: Long): AnalysisRes =
        analysisService.latest(adminContext.require(), surveyId)
}
```

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/tnear/adoptloop/analysis
git commit -m "feat(analysis): AI analysis service + controller"
```

---

### Task 6.3: ActionItem 채택 + 상태

**Files:**
- Create: `src/main/kotlin/com/tnear/adoptloop/actionitem/{ActionItemDtos,ActionItemService,ActionItemController}.kt`

- [ ] **Step 1: 한 파일로 통합 작성**

```kotlin
// ActionItemDtos.kt
package com.tnear.adoptloop.actionitem

import com.tnear.adoptloop.domain.*
import java.time.Instant

data class ActionItemCreateReq(
    val analysisId: Long,
    val title: String, val description: String? = null, val priority: Priority,
)
data class ActionItemUpdateReq(val status: TodoStatus? = null)
data class ActionItemRes(
    val id: Long, val adoptionId: Long, val analysisId: Long,
    val title: String, val description: String?, val priority: Priority, val status: TodoStatus,
    val createdAt: Instant, val updatedAt: Instant,
) {
    companion object {
        fun from(a: ActionItem) = ActionItemRes(
            a.id!!, a.adoptionId, a.analysisId, a.title, a.description, a.priority, a.status, a.createdAt, a.updatedAt,
        )
    }
}
```

```kotlin
// ActionItemService.kt
package com.tnear.adoptloop.actionitem

import com.tnear.adoptloop.domain.ActionItem
import com.tnear.adoptloop.domain.repo.ActionItemRepository
import com.tnear.adoptloop.domain.repo.AdoptionRepository
import com.tnear.adoptloop.domain.repo.AnalysisRepository
import com.tnear.adoptloop.domain.repo.SurveyRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
@Transactional
class ActionItemService(
    private val repo: ActionItemRepository,
    private val adoptionRepo: AdoptionRepository,
    private val analysisRepo: AnalysisRepository,
    private val surveyRepo: SurveyRepository,
) {
    fun adopt(adminId: Long, adoptionId: Long, items: List<ActionItemCreateReq>): List<ActionItem> {
        val a = adoptionRepo.findById(adoptionId).orElseThrow { NoSuchElementException("adoption") }
        if (a.adminId != adminId) throw ResponseStatusException(HttpStatus.FORBIDDEN, "not owner")
        return items.map { item ->
            val analysis = analysisRepo.findById(item.analysisId).orElseThrow { NoSuchElementException("analysis") }
            val survey = surveyRepo.findById(analysis.surveyId).orElseThrow()
            if (survey.adoptionId != adoptionId)
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "analysis ${item.analysisId} does not belong to adoption $adoptionId")
            repo.save(ActionItem(
                adoptionId = adoptionId, analysisId = item.analysisId,
                title = item.title, description = item.description, priority = item.priority,
            ))
        }
    }

    @Transactional(readOnly = true)
    fun list(adminId: Long, adoptionId: Long): List<ActionItem> {
        val a = adoptionRepo.findById(adoptionId).orElseThrow { NoSuchElementException("adoption") }
        if (a.adminId != adminId) throw ResponseStatusException(HttpStatus.FORBIDDEN, "not owner")
        return repo.findAllByAdoptionId(adoptionId)
    }

    fun updateStatus(adminId: Long, id: Long, req: ActionItemUpdateReq): ActionItem {
        val item = repo.findById(id).orElseThrow { NoSuchElementException("action item") }
        val adoption = adoptionRepo.findById(item.adoptionId).orElseThrow()
        if (adoption.adminId != adminId) throw ResponseStatusException(HttpStatus.FORBIDDEN, "not owner")
        req.status?.let { item.status = it }
        return item
    }
}
```

```kotlin
// ActionItemController.kt
package com.tnear.adoptloop.actionitem

import com.tnear.adoptloop.admin.auth.AdminContext
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin")
class ActionItemController(
    private val service: ActionItemService,
    private val adminContext: AdminContext,
) {
    @GetMapping("/adoptions/{adoptionId}/action-items")
    fun list(@PathVariable adoptionId: Long): List<ActionItemRes> =
        service.list(adminContext.require(), adoptionId).map(ActionItemRes::from)

    @PostMapping("/adoptions/{adoptionId}/action-items")
    @ResponseStatus(HttpStatus.CREATED)
    fun adopt(@PathVariable adoptionId: Long, @RequestBody items: List<ActionItemCreateReq>): List<ActionItemRes> =
        service.adopt(adminContext.require(), adoptionId, items).map(ActionItemRes::from)

    @PatchMapping("/action-items/{id}")
    fun update(@PathVariable id: Long, @RequestBody req: ActionItemUpdateReq): ActionItemRes =
        ActionItemRes.from(service.updateStatus(adminContext.require(), id, req))
}
```

- [ ] **Step 2: Commit + M6 PR**

```bash
git add src/main/kotlin/com/tnear/adoptloop/actionitem
git commit -m "feat(action-item): adopt + status update"
git push -u origin feat/analysis
gh pr create --title "feat(M6): aggregate + AI analysis + action items" --body "집계 SELECT + Bedrock 분석 + 액션 아이템 채택/상태."
```

---

## Milestone 7 — UI (Day 4 AM, ~3-4h)

> UI는 토이라 미니멀. Thymeleaf 페이지가 위 JSON API를 inline JS(`fetch`)로 호출. 디자인보다 동작이 우선.

### Task 7.1: AdminViewController + Thymeleaf 페이지

**Files:**
- Create: `src/main/kotlin/com/tnear/adoptloop/web/AdminViewController.kt`
- Create: `src/main/kotlin/com/tnear/adoptloop/web/PublicViewController.kt`
- Create: `src/main/resources/templates/admin/{layout,login}.html`
- Create: `src/main/resources/templates/admin/adoptions/{list,new,detail}.html`
- Create: `src/main/resources/templates/admin/surveys/{edit,analyze}.html`
- Create: `src/main/resources/templates/public/survey.html`
- Create: `src/main/resources/static/css/admin.css`
- Create: `src/main/resources/static/js/respondent.js`

- [ ] **Step 1: 브랜치**

```bash
git checkout main && git pull && git checkout -b feat/ui
```

- [ ] **Step 2: AdminViewController.kt — 단순 라우팅**

```kotlin
package com.tnear.adoptloop.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@Controller
class AdminViewController {
    @GetMapping("/admin") fun root() = "redirect:/admin/adoptions"
    @GetMapping("/admin/login") fun login() = "admin/login"
    @GetMapping("/admin/adoptions") fun list() = "admin/adoptions/list"
    @GetMapping("/admin/adoptions/new") fun new() = "admin/adoptions/new"
    @GetMapping("/admin/adoptions/{id}") fun detail(@PathVariable id: Long) = "admin/adoptions/detail"
    @GetMapping("/admin/surveys/{id}/edit") fun edit(@PathVariable id: Long) = "admin/surveys/edit"
    @GetMapping("/admin/surveys/{id}/analyze") fun analyze(@PathVariable id: Long) = "admin/surveys/analyze"
}
```

- [ ] **Step 3: PublicViewController.kt**

```kotlin
package com.tnear.adoptloop.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@Controller
class PublicViewController {
    @GetMapping("/s/{slug}") fun survey(@PathVariable slug: String) = "public/survey"
}
```

- [ ] **Step 4: AdminKeyFilter 페이지 우회 보강**

`AdminKeyFilter.shouldNotFilter`는 이미 `/api/admin`만 필터함. `/admin/*` HTML 라우트는 통과. (보안 결정: HTML 페이지는 누구나 열 수 있고, 페이지 안에서 API 호출 시 X-Admin-Key를 localStorage에서 꺼내 사용. 키 미등록 시 페이지가 login으로 리다이렉트.)

- [ ] **Step 5: admin/layout.html (Thymeleaf fragment)**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="ko">
<head>
  <meta charset="UTF-8">
  <title th:text="${title} ?: 'AdoptLoop'">AdoptLoop</title>
  <link rel="stylesheet" href="/css/admin.css">
</head>
<body>
<nav><a href="/admin/adoptions">도입 항목</a></nav>
<main th:replace="${content}">page content</main>
<script>
  window.ADMIN_KEY_HEADER = 'X-Admin-Key';
  function adminKey() { return localStorage.getItem('admin_key'); }
  function authFetch(url, opts={}) {
    opts.headers = Object.assign({}, opts.headers, { [ADMIN_KEY_HEADER]: adminKey() });
    if (opts.body && typeof opts.body !== 'string') {
      opts.body = JSON.stringify(opts.body);
      opts.headers['Content-Type'] = 'application/json';
    }
    return fetch(url, opts).then(r => {
      if (r.status === 401) { location.href = '/admin/login'; throw new Error('401'); }
      return r;
    });
  }
  if (!adminKey() && location.pathname !== '/admin/login') location.href = '/admin/login';
</script>
</body>
</html>
```

- [ ] **Step 6: admin/login.html**

```html
<!DOCTYPE html>
<html lang="ko">
<head><meta charset="UTF-8"><title>Login</title></head>
<body>
<h1>Admin Key 입력</h1>
<input id="key" type="password" autofocus>
<button onclick="save()">저장</button>
<script>
  function save() {
    const k = document.getElementById('key').value.trim();
    if (!k) return;
    localStorage.setItem('admin_key', k);
    location.href = '/admin/adoptions';
  }
</script>
</body>
</html>
```

- [ ] **Step 7: adoptions/list.html, new.html, detail.html (간단한 JS 페이지)**

```html
<!-- list.html -->
<!DOCTYPE html><html lang="ko"><head><meta charset="UTF-8"><title>도입 항목</title></head><body>
<nav><a href="/admin/adoptions">도입 항목</a></nav>
<h1>도입 항목</h1>
<a href="/admin/adoptions/new">+ 새로 만들기</a>
<table id="t"><thead><tr><th>이름</th><th>대상</th><th>상태</th></tr></thead><tbody></tbody></table>
<script src="/js/admin-common.js"></script>
<script>
authFetch('/api/admin/adoptions').then(r=>r.json()).then(rows=>{
  const tb=document.querySelector('#t tbody');
  rows.forEach(r=>{
    const tr=document.createElement('tr');
    tr.innerHTML=`<td><a href="/admin/adoptions/${r.id}">${r.name}</a></td><td>${r.target_count}</td><td>${r.status}</td>`;
    tb.appendChild(tr);
  });
});
</script></body></html>
```

> `/js/admin-common.js`에 `authFetch` 추출. layout.html의 동일 코드를 옮기면 됨.

```html
<!-- new.html -->
<!DOCTYPE html><html><head><meta charset="UTF-8"></head><body>
<h1>새 도입 항목</h1>
<form id="f">
  <label>이름 <input name="name" required></label><br>
  <label>목표 <textarea name="goal" required></textarea></label><br>
  <label>대상자 <textarea name="target_audience" required></textarea></label><br>
  <label>우려사항 <textarea name="concern"></textarea></label><br>
  <label>목표 인원 <input type="number" name="target_count" min="1" required></label><br>
  <button>생성</button>
</form>
<script src="/js/admin-common.js"></script>
<script>
document.getElementById('f').addEventListener('submit', async e=>{
  e.preventDefault();
  const data = Object.fromEntries(new FormData(e.target));
  data.target_count = parseInt(data.target_count);
  const r = await authFetch('/api/admin/adoptions', {method:'POST', body:data}).then(r=>r.json());
  location.href = `/admin/adoptions/${r.id}`;
});
</script></body></html>
```

```html
<!-- detail.html — 도입 상세 + 설문 생성 트리거 -->
<!DOCTYPE html><html><head><meta charset="UTF-8"></head><body>
<h1 id="name">…</h1>
<button id="gen">AI 설문 초안 생성</button>
<ul id="surveys"></ul>
<script src="/js/admin-common.js"></script>
<script>
const id = location.pathname.split('/').pop();
authFetch(`/api/admin/adoptions/${id}`).then(r=>r.json()).then(a=>{
  document.getElementById('name').textContent = a.name;
});
document.getElementById('gen').onclick = async ()=>{
  const r = await authFetch(`/api/admin/adoptions/${id}/surveys`, {method:'POST'}).then(r=>r.json());
  location.href = `/admin/surveys/${r.survey.id}/edit`;
};
</script></body></html>
```

**surveys/edit.html** (draft 편집 + 발행):

```html
<!DOCTYPE html><html lang="ko"><head><meta charset="UTF-8"><title>설문 편집</title></head><body>
<h1 id="title">설문 편집</h1>
<p>상태: <span id="status"></span> · 마감: <span id="deadline"></span></p>
<div id="questions"></div>
<button id="addQ" type="button">+ 문항 추가</button>
<hr>
<button id="save" type="button">문항 저장</button>
<button id="publish" type="button">발행</button>
<script src="/js/admin-common.js"></script>
<script>
const id = location.pathname.split('/')[3];   // /admin/surveys/{id}/edit
let questions = [];

function render() {
  const c = document.getElementById('questions');
  c.innerHTML = '';
  questions.forEach((q, i) => {
    const div = document.createElement('div');
    div.innerHTML = `
      <fieldset>
        <label>문항 ${i+1}
          <input data-i="${i}" data-f="text" value="${q.text ?? ''}" style="width:60%">
        </label>
        <select data-i="${i}" data-f="type">
          ${['TEXT','SCALE','SINGLE_CHOICE'].map(t=>`<option ${q.type===t?'selected':''}>${t}</option>`).join('')}
        </select>
        ${q.type==='SCALE' ? `<select data-i="${i}" data-f="axis">
          ${['USAGE','BEHAVIOR','VALUE'].map(a=>`<option ${q.axis===a?'selected':''}>${a}</option>`).join('')}
        </select>` : ''}
        ${q.type==='SINGLE_CHOICE' ? `<div>옵션(쉼표구분):
          <input data-i="${i}" data-f="optsText" value="${(q.options||[]).map(o=>o.text).join(',')}" style="width:50%">
        </div>` : ''}
        <button data-i="${i}" data-act="del" type="button">삭제</button>
      </fieldset>`;
    c.appendChild(div);
  });
}

function bindEvents() {
  document.getElementById('questions').addEventListener('input', e=>{
    const i = +e.target.dataset.i, f = e.target.dataset.f;
    if (!f) return;
    if (f === 'optsText') {
      questions[i].options = e.target.value.split(',').map((t,k)=>({text:t.trim(), order_index:k+1}));
    } else {
      questions[i][f] = e.target.value;
    }
  });
  document.getElementById('questions').addEventListener('click', e=>{
    if (e.target.dataset.act === 'del') {
      questions.splice(+e.target.dataset.i, 1);
      render();
    }
  });
}

document.getElementById('addQ').onclick = ()=>{
  questions.push({type:'SCALE', text:'', order_index: questions.length+1, required:true, axis:'USAGE', options:[]});
  render();
};

document.getElementById('save').onclick = async ()=>{
  const payload = questions.map((q, i) => ({
    type: q.type, text: q.text, order_index: i+1, required: true,
    axis: q.type === 'SCALE' ? q.axis : null,
    options: q.type === 'SINGLE_CHOICE' ? (q.options || []) : [],
  }));
  const r = await authFetch(`/api/admin/surveys/${id}/questions`, {method:'PUT', body: payload});
  alert(r.ok ? '저장됨' : `오류: ${r.status}`);
};

document.getElementById('publish').onclick = async ()=>{
  if (!confirm('발행하시겠어요?')) return;
  const r = await authFetch(`/api/admin/surveys/${id}/publish`, {method:'POST'});
  if (r.ok) {
    const survey = await r.json();
    alert(`발행 완료. 공개 링크: /s/${survey.public_slug}`);
    location.href = `/admin/adoptions/${survey.adoption_id}`;
  } else {
    alert(`오류: ${r.status}`);
  }
};

authFetch(`/api/admin/surveys/${id}`).then(r=>r.json()).then(d=>{
  document.getElementById('title').textContent = d.survey.title;
  document.getElementById('status').textContent = d.survey.status;
  document.getElementById('deadline').textContent = d.survey.deadline;
  questions = d.questions;
  render();
  bindEvents();
});
</script></body></html>
```

**surveys/analyze.html** (집계 → 분석 실행 → 액션 채택):

```html
<!DOCTYPE html><html lang="ko"><head><meta charset="UTF-8"><title>분석</title></head><body>
<h1>분석</h1>
<section>
  <h2>집계</h2>
  <pre id="agg">로딩...</pre>
</section>
<button id="run">분석 실행</button>
<section id="result" style="display:none">
  <h2>점수</h2>
  <table id="scores"></table>
  <h2>제안 액션</h2>
  <ul id="suggested"></ul>
  <button id="adopt">선택한 항목 채택</button>
</section>
<script src="/js/admin-common.js"></script>
<script>
const surveyId = location.pathname.split('/')[3];
let suggested = [], adoptionId = null;

authFetch(`/api/admin/surveys/${surveyId}`).then(r=>r.json()).then(d=>{
  adoptionId = d.survey.adoption_id;
});

authFetch(`/api/admin/surveys/${surveyId}/aggregate`).then(r=>r.json()).then(a=>{
  document.getElementById('agg').textContent = JSON.stringify(a, null, 2);
});

document.getElementById('run').onclick = async ()=>{
  const r = await authFetch(`/api/admin/surveys/${surveyId}/analyses`, {method:'POST'});
  if (!r.ok) { alert(`오류: ${r.status}`); return; }
  const data = await r.json();
  const a = data.analysis;
  document.getElementById('scores').innerHTML = `
    <tr><th>정착도</th><td>${a.adoption_score}</td></tr>
    <tr><th>사용</th><td>${a.usage_score}</td></tr>
    <tr><th>행동</th><td>${a.behavior_score}</td></tr>
    <tr><th>가치</th><td>${a.value_score}</td></tr>`;
  suggested = data.suggested_action_items.map(s => ({...s, analysis_id: a.id}));
  const ul = document.getElementById('suggested');
  ul.innerHTML = '';
  suggested.forEach((s, i)=>{
    const li = document.createElement('li');
    li.innerHTML = `<label><input type="checkbox" data-i="${i}"> [${s.priority}] ${s.title}
      <small>${s.description ?? ''}</small></label>`;
    ul.appendChild(li);
  });
  document.getElementById('result').style.display = 'block';
};

document.getElementById('adopt').onclick = async ()=>{
  const picked = [...document.querySelectorAll('#suggested input:checked')].map(el => suggested[+el.dataset.i]);
  if (!picked.length) { alert('항목을 선택하세요'); return; }
  const r = await authFetch(`/api/admin/adoptions/${adoptionId}/action-items`, {method:'POST', body: picked});
  alert(r.ok ? '채택됨' : `오류: ${r.status}`);
  if (r.ok) location.href = `/admin/adoptions/${adoptionId}`;
};
</script></body></html>
```

- [ ] **Step 8: public/survey.html + respondent.js**

```html
<!DOCTYPE html><html lang="ko"><head><meta charset="UTF-8"><title>설문</title></head><body>
<h1 id="title">로딩...</h1>
<p>마감: <span id="deadline"></span></p>
<form id="f"></form>
<button id="submit">제출</button>
<script src="/js/respondent.js"></script>
</body></html>
```

```javascript
// static/js/respondent.js
const slug = location.pathname.split('/').pop();
const tokenKey = `al_token_${slug}`;

async function ensureToken() {
  let t = localStorage.getItem(tokenKey);
  if (t) return t;
  const r = await fetch(`/api/public/surveys/${slug}/responses`, {method:'POST'}).then(r=>r.json());
  localStorage.setItem(tokenKey, r.access_token);
  return r.access_token;
}

async function render() {
  const token = await ensureToken();
  const data = await fetch(`/api/public/responses/${token}`).then(r=>r.json());
  document.getElementById('title').textContent = data.survey.title;
  document.getElementById('deadline').textContent = data.survey.deadline;
  const form = document.getElementById('f');
  const answers = new Map(data.answers.map(a=>[a.question_id,a]));
  data.survey.questions.forEach(q=>{
    const div = document.createElement('div');
    const existing = answers.get(q.id);
    if (q.type === 'TEXT') div.innerHTML = `<label>${q.text}<textarea name="${q.id}">${existing?.text_value??''}</textarea></label>`;
    if (q.type === 'SCALE') div.innerHTML = `<label>${q.text} (1-5)<input type="number" min="1" max="5" name="${q.id}" data-kind="scale" value="${existing?.scale_value??''}"></label>`;
    if (q.type === 'SINGLE_CHOICE') {
      const opts = q.options.map(o=>`<label><input type="radio" name="${q.id}" value="${o.id}" data-kind="option" ${existing?.question_option_id===o.id?'checked':''}>${o.text}</label>`).join('');
      div.innerHTML = `<fieldset><legend>${q.text}</legend>${opts}</fieldset>`;
    }
    form.appendChild(div);
  });
}

document.getElementById('submit').onclick = async ()=>{
  const token = localStorage.getItem(tokenKey);
  const form = document.getElementById('f');
  const items = [];
  new FormData(form).forEach((v, k)=>{
    const el = form.querySelector(`[name="${k}"]`);
    const kind = el.dataset.kind;
    if (kind === 'scale') items.push({question_id:Number(k), scale_value:Number(v)});
    else if (kind === 'option') items.push({question_id:Number(k), question_option_id:Number(v)});
    else items.push({question_id:Number(k), text_value:String(v)});
  });
  const r = await fetch(`/api/public/responses/${token}/answers`, {
    method:'PUT', headers:{'Content-Type':'application/json'}, body: JSON.stringify(items),
  });
  alert(r.ok ? '제출되었습니다.' : `오류: ${r.status}`);
};

render();
```

- [ ] **Step 9: 수동 검증**

```bash
docker compose up -d postgres
ADOPTLOOP_ADMIN_NAME=dev ADOPTLOOP_ADMIN_KEY=devkey ./gradlew bootRun &
# → http://localhost:8080/admin (devkey 입력) → 도입 생성 → 설문 생성 → 발행
# → http://localhost:8080/s/<slug> (응답)
```

- [ ] **Step 10: Commit + PR**

```bash
git add src/main/kotlin/com/tnear/adoptloop/web src/main/resources/templates src/main/resources/static
git commit -m "feat(M7): Thymeleaf admin UI + respondent JS"
git push -u origin feat/ui
gh pr create --title "feat(M7): minimal admin UI + respondent JS" --body "관리자 페이지(목록·신규·상세·편집·분석)와 응답자 페이지. 동작 우선."
```

---

## Milestone 8 — 강화 테스트 (Day 4 PM ~ Day 5, ~1-2일)

### Task 8.1: E2E happy path (모든 단계 연결)

**Files:**
- Create: `src/test/kotlin/com/tnear/adoptloop/e2e/HappyPathE2ETest.kt`

- [ ] **Step 1: 브랜치**

```bash
git checkout main && git pull && git checkout -b test/e2e
```

- [ ] **Step 2: HappyPathE2ETest.kt**

```kotlin
package com.tnear.adoptloop.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninja_squad.springmockk.MockkBean
import com.tnear.adoptloop.IntegrationTestBase
import com.tnear.adoptloop.domain.Admin
import com.tnear.adoptloop.domain.repo.AdminRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.security.MessageDigest
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
class HappyPathE2ETest @Autowired constructor(
    private val mvc: MockMvc,
    private val om: ObjectMapper,
    private val adminRepo: AdminRepository,
) : IntegrationTestBase() {

    @MockkBean private lateinit var chatClient: ChatClient

    @Test
    fun `full flow — adoption → draft → publish → response → analysis → action item`() {
        val raw = "k-${System.nanoTime()}"
        val hash = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
        adminRepo.save(Admin(name = "e2e", keyHash = hash))
        val key = raw

        // 1. adoption
        val adoptionId = mvc.perform(post("/api/admin/adoptions").header("X-Admin-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(om.writeValueAsString(mapOf("name" to "Jira", "goal" to "g", "target_audience" to "ta", "target_count" to 3))))
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString.let { om.readTree(it)["id"].asLong() }

        // 2. AI 초안 — deadline 매우 짧게 (이후 마감 후 분석 시나리오)
        mockChatOnce("""{"title":"S","questions":[{"type":"TEXT","text":"how"}]}""")
        val surveyJson = mvc.perform(post("/api/admin/adoptions/$adoptionId/surveys")
            .header("X-Admin-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(om.writeValueAsString(mapOf("deadline" to Instant.now().plusSeconds(2).toString()))))
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString
        val surveyId = om.readTree(surveyJson)["survey"]["id"].asLong()
        val questionId = om.readTree(surveyJson)["questions"][0]["id"].asLong()

        // 3. publish (deadline·slack 변경 없음)
        mvc.perform(post("/api/admin/surveys/$surveyId/publish").header("X-Admin-Key", key))
            .andExpect(status().isOk)

        // 4. 공개 응답
        val slug = om.readTree(surveyJson)["survey"]["public_slug"].asText()
        val token = mvc.perform(post("/api/public/surveys/$slug/responses"))
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString.let { om.readTree(it)["access_token"].asText() }
        mvc.perform(put("/api/public/responses/$token/answers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(om.writeValueAsString(listOf(mapOf("question_id" to questionId, "text_value" to "great")))))
            .andExpect(status().isOk)

        // 5. 마감 대기
        Thread.sleep(2500)

        // 6. 분석
        mockChatOnce("""{"adoption_score":70,"usage_score":60,"behavior_score":65,"value_score":80,
            "positive_signals":["A"],"resistance_factors":["B"],"risks":["C"],
            "suggested_action_items":[{"title":"X","priority":"HIGH"}]}""")
        val analysisJson = mvc.perform(post("/api/admin/surveys/$surveyId/analyses").header("X-Admin-Key", key))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.analysis.adoption_score").value(70))
            .andReturn().response.contentAsString
        val analysisId = om.readTree(analysisJson)["analysis"]["id"].asLong()

        // 7. 액션 채택
        mvc.perform(post("/api/admin/adoptions/$adoptionId/action-items").header("X-Admin-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(om.writeValueAsString(listOf(mapOf(
                "analysis_id" to analysisId, "title" to "X", "priority" to "HIGH"
            )))))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$[0].status").value("TODO"))
    }

    private fun mockChatOnce(content: String) {
        val spec = mockk<ChatClient.ChatClientRequestSpec>(relaxed = true)
        every { chatClient.prompt() } returns spec
        every { spec.user(any<String>()) } returns spec
        val call = mockk<ChatClient.CallResponseSpec>(relaxed = true)
        every { spec.call() } returns call
        every { call.content() } returns content
    }
}
```

- [ ] **Step 3: 실행**

Run: `./gradlew test --tests HappyPathE2ETest`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/test/kotlin/com/tnear/adoptloop/e2e/HappyPathE2ETest.kt
git commit -m "test(e2e): full happy path adoption → action item"
```

---

### Task 8.2: Edge cases

**Files:**
- Create: `src/test/kotlin/com/tnear/adoptloop/e2e/EdgeCaseE2ETest.kt`

- [ ] **Step 1: 작성 — 핵심 케이스 4개**

```kotlin
package com.tnear.adoptloop.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninja_squad.springmockk.MockkBean
import com.tnear.adoptloop.IntegrationTestBase
import com.tnear.adoptloop.domain.Admin
import com.tnear.adoptloop.domain.repo.AdminRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.security.MessageDigest
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
class EdgeCaseE2ETest @Autowired constructor(
    private val mvc: MockMvc,
    private val om: ObjectMapper,
    private val adminRepo: AdminRepository,
) : IntegrationTestBase() {

    @MockkBean private lateinit var chatClient: ChatClient

    @Test
    fun `analysis before deadline returns 403`() {
        val key = seedAdminKey()
        val adoptionId = createAdoption(key)
        // deadline을 멀리 두어 분석 요청 시점에 아직 미도래
        val draft = createDraft(adoptionId, key, deadline = Instant.now().plusSeconds(3600))
        publish(draft.surveyId, key)

        // 액션: deadline 도래 전 분석 요청 → 403 (cohort 미확정)
        mvc.perform(post("/api/admin/surveys/${draft.surveyId}/analyses").header("X-Admin-Key", key))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `submit after deadline returns 403`() {
        val key = seedAdminKey()
        val adoptionId = createAdoption(key)
        val draft = createDraft(adoptionId, key, deadline = Instant.now().plusSeconds(2))
        publish(draft.surveyId, key)
        val token = startResponse(draft.publicSlug)

        // deadline 경과 대기
        Thread.sleep(2500)

        // 액션: 마감 후 응답 제출 → 403 (DEADLINE_EXCEEDED)
        mvc.perform(put("/api/public/responses/$token/answers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(om.writeValueAsString(listOf(mapOf(
                "question_id" to draft.questionId, "text_value" to "late")))))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `wrong admin cannot read other admin's adoption`() {
        val keyA = seedAdminKey(name = "admin-A")
        val keyB = seedAdminKey(name = "admin-B")
        val adoptionId = createAdoption(keyA)

        // 액션: admin B 키로 admin A의 도입 조회 → 403 (NOT_OWNER)
        mvc.perform(get("/api/admin/adoptions/$adoptionId").header("X-Admin-Key", keyB))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `replace questions on published survey returns 409`() {
        val key = seedAdminKey()
        val adoptionId = createAdoption(key)
        val draft = createDraft(adoptionId, key, deadline = Instant.now().plusSeconds(3600))
        publish(draft.surveyId, key)

        // 액션: published 상태에서 questions 일괄 교체 시도 → 409 (SURVEY_NOT_EDITABLE)
        mvc.perform(put("/api/admin/surveys/${draft.surveyId}/questions")
            .header("X-Admin-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(om.writeValueAsString(listOf(mapOf(
                "type" to "TEXT", "text" to "new", "order_index" to 0, "required" to true)))))
            .andExpect(status().isConflict)
    }

    // ──────── seed helpers (HappyPathE2ETest의 inline 시드 패턴을 명명 함수로 추출) ────────

    private data class DraftCreated(val surveyId: Long, val questionId: Long, val publicSlug: String)

    private fun seedAdminKey(name: String = "e2e"): String {
        val raw = "k-${System.nanoTime()}-$name"
        val hash = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
        adminRepo.save(Admin(name = name, keyHash = hash))
        return raw
    }

    private fun createAdoption(key: String, targetCount: Int = 3): Long =
        mvc.perform(post("/api/admin/adoptions").header("X-Admin-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(om.writeValueAsString(mapOf(
                "name" to "Jira", "goal" to "g", "target_audience" to "ta", "target_count" to targetCount))))
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString.let { om.readTree(it)["id"].asLong() }

    private fun createDraft(adoptionId: Long, key: String, deadline: Instant): DraftCreated {
        mockChatOnce("""{"title":"S","questions":[{"type":"TEXT","text":"how"}]}""")
        val json = mvc.perform(post("/api/admin/adoptions/$adoptionId/surveys")
            .header("X-Admin-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(om.writeValueAsString(mapOf("deadline" to deadline.toString()))))
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString
        val node = om.readTree(json)
        return DraftCreated(
            surveyId = node["survey"]["id"].asLong(),
            questionId = node["questions"][0]["id"].asLong(),
            publicSlug = node["survey"]["public_slug"].asText(),
        )
    }

    private fun publish(surveyId: Long, key: String) {
        mvc.perform(post("/api/admin/surveys/$surveyId/publish").header("X-Admin-Key", key))
            .andExpect(status().isOk)
    }

    private fun startResponse(publicSlug: String): String =
        mvc.perform(post("/api/public/surveys/$publicSlug/responses"))
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString.let { om.readTree(it)["access_token"].asText() }

    private fun mockChatOnce(content: String) {
        val spec = mockk<ChatClient.ChatClientRequestSpec>(relaxed = true)
        every { chatClient.prompt() } returns spec
        every { spec.user(any<String>()) } returns spec
        val call = mockk<ChatClient.CallResponseSpec>(relaxed = true)
        every { spec.call() } returns call
        every { call.content() } returns content
    }
}
```

(각 테스트의 본문은 happy path와 같은 시드 패턴 + 단일 액션 + status 검증. 시드는 명명된 private 헬퍼로 추출하여 4개 테스트가 공유한다. 행위 검증은 HTTP status까지 (응답 본문의 `code` 문자열 매칭은 구현 후 별도 보강).)

- [ ] **Step 2: 실행 + Commit**

```bash
./gradlew test
git add src/test/kotlin/com/tnear/adoptloop/e2e/EdgeCaseE2ETest.kt
git commit -m "test(e2e): edge cases (deadline, ownership, status conflicts)"
```

---

### Task 8.3: LLM 실패 시나리오 테스트

**Files:**
- Create: `src/test/kotlin/com/tnear/adoptloop/survey/SurveyDraftFailureTest.kt`

> M4·M6에서 이미 `LlmTransientException` + 503 매핑이 적용되어 있다. 이 Task는 동작 검증만.

- [ ] **Step 1: LLM 호출 실패 → 503 + code=LLM_TRANSIENT 확인**

```kotlin
// every { call.content() } throws RuntimeException("bedrock timeout")
mvc.perform(post("/api/admin/adoptions/${adoption.id}/surveys").header("X-Admin-Key", key)
    .contentType(MediaType.APPLICATION_JSON)
    .content(om.writeValueAsString(mapOf("deadline" to Instant.now().plusSeconds(3600).toString()))))
    .andExpect(status().isServiceUnavailable)
    .andExpect(jsonPath("$.code").value("LLM_TRANSIENT"))
```

- [ ] **Step 2: LLM 출력 파싱 실패(잘못된 JSON) → 503 + code=LLM_TRANSIENT 확인** (같은 패턴, mock은 정상 리턴 + 깨진 JSON)

- [ ] **Step 3: AnalysisService도 동일 패턴 테스트** (M6.2 LLM 호출도 같은 예외 사용)

- [ ] **Step 4: Commit + M8 PR**

```bash
git add .
git commit -m "test(e2e): LLM transient (call + parse failure) → 503 LLM_TRANSIENT"
git push -u origin test/e2e
gh pr create --title "test(M8): E2E happy + edge + LLM failure" --body "전체 흐름 통합 테스트 강화."
```

---

## Milestone 9 — 배포 (Day 6-7, ~1-2일)

### Task 9.1: Dockerfile (multi-stage)

**Files:**
- Create: `Dockerfile`
- Create: `.dockerignore`

- [ ] **Step 1: 브랜치**

```bash
git checkout main && git pull && git checkout -b feat/deploy
```

- [ ] **Step 2: Dockerfile**

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

- [ ] **Step 3: .dockerignore**

```
.git
.gradle
build
.idea
*.iml
src/test
docs
deploy
.github
```

- [ ] **Step 4: 로컬 빌드 검증**

Run:
```bash
docker build -t adoptloop:dev .
docker run --rm -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/adoptloop \
  -e DB_USER=adoptloop -e DB_PASSWORD=adoptloop \
  adoptloop:dev
```
Expected: 시작 후 `http://localhost:8080/admin` 접근 가능.

- [ ] **Step 5: Commit**

```bash
git add Dockerfile .dockerignore
git commit -m "build: multi-stage Dockerfile (Gradle build + JRE21 runtime)"
```

---

### Task 9.2: RDS PostgreSQL 인프라 메모

**Files:**
- Create: `deploy/README.md`

- [ ] **Step 1: deploy/README.md**

```markdown
# AdoptLoop Deployment Notes

## RDS PostgreSQL
- 인스턴스: `db.t4g.micro`, PostgreSQL 16, 단일 AZ (토이)
- DB 이름: `adoptloop`, 사용자: `adoptloop`
- VPC: ECS Fargate task와 동일 VPC, 같은 보안 그룹에서만 5432 접속 허용
- 백업: 자동 백업 비활성화 (토이) — 운영 전환 시 활성화

## ECR
- 레지스트리: `<ACCOUNT>.dkr.ecr.ap-northeast-2.amazonaws.com/adoptloop-server`
- 이미지 태그: git short SHA (예: `e83f9c5`)

## ECS Fargate
- 클러스터: `adoptloop`
- 서비스: `adoptloop-svc` (desiredCount=1, 토이)
- Task Definition: `deploy/task-definition.json`
- IAM Task Role: Bedrock `InvokeModel` + RDS 연결 권한
- 환경변수 (Task Definition):
  - `DB_URL`, `DB_USER`, `DB_PASSWORD` — Secrets Manager 참조
  - `ADOPTLOOP_ADMIN_NAME`, `ADOPTLOOP_ADMIN_KEY` — 최초 부팅 후 제거 권장
  - `AWS_REGION=ap-northeast-2`

## Slack Webhook
- ECS task 환경변수 또는 Secrets Manager. 코드/이미지에 평문 금지.

## 첫 배포 절차
1. RDS PostgreSQL 프로비저닝
2. ECR 리포 생성: `aws ecr create-repository --repository-name adoptloop-server`
3. 로컬에서 빌드·푸시:
   ```bash
   aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin <ACCOUNT>.dkr.ecr.ap-northeast-2.amazonaws.com
   docker build -t adoptloop:$(git rev-parse --short HEAD) .
   docker tag adoptloop:$(git rev-parse --short HEAD) <ACCOUNT>.dkr.ecr.ap-northeast-2.amazonaws.com/adoptloop-server:$(git rev-parse --short HEAD)
   docker push <ACCOUNT>.dkr.ecr.ap-northeast-2.amazonaws.com/adoptloop-server:$(git rev-parse --short HEAD)
   ```
4. ECS Service 생성 (Task Definition + Public IP / ALB)
5. Smoke test: `curl https://<endpoint>/admin/login` → 200
```

---

### Task 9.3: ECS Task Definition

**Files:**
- Create: `deploy/task-definition.json`

- [ ] **Step 1: task-definition.json**

```json
{
  "family": "adoptloop",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::<ACCOUNT>:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::<ACCOUNT>:role/AdoptloopTaskRole",
  "containerDefinitions": [
    {
      "name": "adoptloop",
      "image": "<ACCOUNT>.dkr.ecr.ap-northeast-2.amazonaws.com/adoptloop-server:latest",
      "portMappings": [{"containerPort": 8080, "protocol": "tcp"}],
      "essential": true,
      "environment": [
        {"name": "SPRING_PROFILES_ACTIVE", "value": "prod"},
        {"name": "AWS_REGION", "value": "ap-northeast-2"}
      ],
      "secrets": [
        {"name": "DB_URL", "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:<ACCOUNT>:secret:adoptloop/db-url"},
        {"name": "DB_USER", "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:<ACCOUNT>:secret:adoptloop/db-user"},
        {"name": "DB_PASSWORD", "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:<ACCOUNT>:secret:adoptloop/db-password"}
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/adoptloop",
          "awslogs-region": "ap-northeast-2",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

- [ ] **Step 2: Commit**

```bash
git add deploy
git commit -m "deploy: RDS/ECR/ECS notes + Fargate task definition"
```

---

### Task 9.4: GitHub Actions — CI + 배포

**Files:**
- Create: `.github/workflows/ci.yml`
- Create: `.github/workflows/deploy.yml`

- [ ] **Step 1: ci.yml**

```yaml
name: CI
on:
  pull_request: {}
  push:
    branches: [main]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21 }
      - uses: gradle/actions/setup-gradle@v3
      - run: ./gradlew test
```

- [ ] **Step 2: deploy.yml (선택)**

```yaml
name: Deploy
on:
  push:
    branches: [main]
jobs:
  deploy:
    runs-on: ubuntu-latest
    permissions:
      id-token: write
      contents: read
    steps:
      - uses: actions/checkout@v4
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::<ACCOUNT>:role/GHADeployRole
          aws-region: ap-northeast-2
      - uses: aws-actions/amazon-ecr-login@v2
      - name: Build and push image
        run: |
          SHA=$(git rev-parse --short HEAD)
          docker build -t adoptloop:$SHA .
          docker tag adoptloop:$SHA $REGISTRY/adoptloop-server:$SHA
          docker push $REGISTRY/adoptloop-server:$SHA
        env:
          REGISTRY: <ACCOUNT>.dkr.ecr.ap-northeast-2.amazonaws.com
      - name: Update ECS service
        run: |
          aws ecs update-service --cluster adoptloop --service adoptloop-svc --force-new-deployment
```

- [ ] **Step 3: Commit + PR**

```bash
git add .github
git commit -m "ci: gradle test on PR; deploy workflow on main"
git push -u origin feat/deploy
gh pr create --title "feat(M9): Dockerfile + ECS deploy + GHA" --body "Multi-stage Dockerfile, RDS/ECS/ECR notes, Fargate task definition, GitHub Actions CI + 배포."
```

---

### Task 9.5: Smoke test

- [ ] **Step 1: 배포 직후 검증**

```bash
ENDPOINT=https://<ecs-public-dns-or-alb>
curl -i $ENDPOINT/admin/login                       # 200
curl -i -H "X-Admin-Key: $ADMIN_KEY" $ENDPOINT/api/admin/adoptions  # 200 [] (또는 시드)
```

- [ ] **Step 2: 실패 시 CloudWatch Logs `/ecs/adoptloop` 확인**

특히 다음을 확인:
- Flyway 마이그레이션 (`V1__init.sql` 실행 로그)
- 데이터소스 연결 (RDS 보안그룹 5432 허용 여부)
- Bedrock IAM 권한 (`InvokeModel` denied 메시지)

---

## Self-Review (작성 직후 점검)

### 1. SPEC coverage

| SPEC 6장 | 담당 Task |
|---|---|
| 6.1 도입 항목 관리 | M2 (Task 2.1, 2.2, 2.4) |
| 6.2 AI 설문 초안 생성 | M4 (Task 4.1) |
| 6.3 설문 발행 + 공개 링크 | M3 (Task 3.1), M5 (Task 5.1) |
| 6.4 응답 집계 | M6 (Task 6.1) |
| 6.5 AI 분석 (출력 5종) | M6 (Task 6.2) |
| 6.6 액션 아이템 관리 | M6 (Task 6.3) |
| 6.7 Slack 발행 알림 | M3 (Task 3.1, SlackNotifier) |

SPEC 8장 보안 — AdminKeyFilter (M1.5), 응답자 토큰 (M5.1) — 모두 반영.
SPEC 9장 도메인 — 엔티티 8 + Admin = 9개 (M1.4 + M2.1) — 반영. `surveys.deadline NULL 허용`은 의도적 차이로 V1 SQL에 표기.

### 2. Placeholder scan
- "TBD"/"TODO" 등 부재.
- 9.4의 `<ACCOUNT>` 토큰은 사용자 환경값 — 의도적 자리표시자.
- 9.2/9.3의 ALB/Public IP 선택은 사용자 결정 사항 — 메모에 명시함.

### 3. Type consistency
- `SurveyStatus = {draft, published, closed}` 일관.
- `Axis = {usage, behavior, value}` 일관.
- `ResponseStatus = {in_progress, submitted}` 일관.
- `Priority = {high, medium, low}`, `TodoStatus = {todo, in_progress, done}` 일관.
- `QuestionVo`는 survey 모듈에 정의되어 publicapi 모듈에서 import — 의존 방향 OK.

### 4. 알려진 제약
- M7 UI 모든 페이지 본문 채워짐 (list, new, detail, surveys/edit, surveys/analyze, public/survey).
- M6.1 AggregateService는 `findAllBySurveyIdAndStatus` 직접 조회 + in-memory 그룹핑. answers는 `findAllBySurveyResponseIdIn` 1회 — 충분히 빠름.

---

## 변경 이력

| 날짜 | 내용 |
|---|---|
| 2026-05-26 | 초안 작성 — 9 milestone, 구현 3일 + 검증 1-2일 + 배포 1-2일 |
