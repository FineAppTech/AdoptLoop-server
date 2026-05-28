# ADR + PROJECT_CONTEXT Seed Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Seed `docs/PROJECT_CONTEXT.md` (project orientation + ADR index) and `docs/adr/` (8 backfill ADRs + template + index) into the AdoptLoop repository, with `CLAUDE.md` updated to auto-load the project context.

**Architecture:** All documentation files. No source code changes. Each task creates or modifies one logical unit and commits, so the resulting PR is reviewable commit-by-commit. The 8 backfilled ADRs are grouped into 5 tasks by topical affinity to keep commits small but coherent.

**Tech Stack:** Markdown only. Nygard classic ADR format. Claude Code `@<path>` import syntax in `CLAUDE.md`.

**Working branch:** `docs/adr-and-project-context-seed` (already created and contains the design spec commit `1f7517c`).

**Reference spec:** `docs/superpowers/specs/2026-05-28-adr-and-project-context-design.md`

---

## File Structure

Files to create (all under repo root `/Users/tnear/tnear/toy-projects/adoptloop-server/`):

| Path | Purpose |
|---|---|
| `docs/PROJECT_CONTEXT.md` | Project orientation + ADR index pointer + current state. Auto-loaded via CLAUDE.md `@import`. |
| `docs/adr/README.md` | ADR index table. Manually appended to whenever a new ADR is added. |
| `docs/adr/0000-template.md` | Copy-paste template for new ADRs. Nygard classic format. |
| `docs/adr/0001-enum-uppercase-convention.md` | Backfill: enum uppercase across DB/JSON/LLM. |
| `docs/adr/0002-survey-deadline-required.md` | Backfill: `deadline NOT NULL` at draft creation. |
| `docs/adr/0003-jackson-snake-case-and-dto-naming.md` | Backfill: global SNAKE_CASE + DTO suffix convention. |
| `docs/adr/0004-error-mapping-via-response-status-exception.md` | Backfill: `ResponseStatusException` everywhere. |
| `docs/adr/0005-slack-webhook-as-server-config.md` | Backfill: drop `surveys.slack_webhook_url` column; config-only. |
| `docs/adr/0006-llm-call-outside-transaction-and-503-mapping.md` | Backfill: LLM outside `@Transactional`; `LlmTransientException` → 503. |
| `docs/adr/0007-response-validation-policy.md` | Backfill: required/cap/empty-text rules for public response endpoint. |
| `docs/adr/0008-ecs-deployment-latest-tag-force-new.md` | Backfill: `:latest` + `force-new-deployment` (toy trade-off). |

Files to modify:

| Path | Change |
|---|---|
| `CLAUDE.md` | Prepend `@docs/PROJECT_CONTEXT.md` import line + blank line. No other change. |

External (outside repo) file to update at the end:

| Path | Change |
|---|---|
| `/Users/tnear/.claude/projects/-Users-tnear-tnear-toy-projects-adoptloop-server/memory/adoptloop-spec-progress.md` | Append note that ADR/PROJECT_CONTEXT seed is complete. |

---

## Pre-flight

Verify you're on the right branch before starting. The branch was created during brainstorming.

- [ ] **Step 0: Confirm branch and clean working state for plan files**

Run:
```bash
git branch --show-current
git status --short
```

Expected:
- Current branch: `docs/adr-and-project-context-seed`
- `git status` shows the unrelated pre-existing changes (`M docs/AdoptLoop_API.yaml`, `?? docs/AdoptLoop_PLAN.md`) — **do not touch these**.
- `docs/superpowers/specs/2026-05-28-adr-and-project-context-design.md` is already committed.

If branch is wrong: `git checkout docs/adr-and-project-context-seed`.

---

## Task 1: ADR scaffold (folder + template + empty index)

**Files:**
- Create: `docs/adr/0000-template.md`
- Create: `docs/adr/README.md`

- [ ] **Step 1.1: Create the ADR template**

Create `docs/adr/0000-template.md` with exactly this content:

````markdown
# ADR-XXXX: <짧은 제목>

- 상태: Proposed | Accepted | Superseded by ADR-YYYY | Deprecated
- 날짜: YYYY-MM-DD
- 관련: (예: M3 Survey publication, 또는 ADR-0005)

## Context
이 결정을 내리게 된 배경. 어떤 상황·제약·문제가 있었는지.

## Decision
무엇을 결정했는가. 구체적·검증 가능하게.

## Consequences
- ✅ 긍정적 영향
- ❌ 부정적 영향·트레이드오프
- 추후 다시 봐야 할 조건이 있다면 명시.
````

- [ ] **Step 1.2: Create the empty ADR index**

Create `docs/adr/README.md` with exactly this content:

````markdown
# ADR Index

> Architecture Decision Records — AdoptLoop의 SPEC/TECH_STACK 편집급 결정 기록.
> 새 ADR 작성 규칙은 `docs/PROJECT_CONTEXT.md` 참조.

| # | 제목 | 상태 | 한 줄 요약 |
|---|---|---|---|
| (ADR가 추가되면 여기에 한 줄씩 append) |
````

- [ ] **Step 1.3: Verify files**

Run:
```bash
ls -la docs/adr/
test -s docs/adr/0000-template.md && echo "template OK"
test -s docs/adr/README.md && echo "index OK"
```

Expected:
- Both files exist and are non-empty.
- Output includes `template OK` and `index OK`.

- [ ] **Step 1.4: Commit**

```bash
git add docs/adr/0000-template.md docs/adr/README.md
git commit -m "$(cat <<'EOF'
docs(adr): ADR 스캐폴드 (Nygard 템플릿 + 빈 인덱스)

docs/adr/ 폴더와 0000-template.md, README.md(빈 인덱스 표)를 추가.
이후 ADR은 이 템플릿을 복사해 작성하고 README 표에 한 줄 append.
EOF
)"
```

---

## Task 2: PROJECT_CONTEXT.md

**Files:**
- Create: `docs/PROJECT_CONTEXT.md`

- [ ] **Step 2.1: Create the project context file**

Create `docs/PROJECT_CONTEXT.md` with exactly this content:

````markdown
# AdoptLoop — Project Context

> 이 파일은 `CLAUDE.md`의 `@import`로 자동 로드된다. 새 세션·새 작업자의 진입점.

## 이 프로젝트는?
AdoptLoop는 설문 작성 → 발행 → 응답 수집 → LLM 분석 → ActionItem 생성까지 자동화하는 7일 토이 프로젝트.
- 백엔드: Spring Boot 4 + Kotlin + PostgreSQL
- AI: AWS Bedrock — Claude Haiku 4.5 (Spring AI Converse API)
- UI: 관리자 = Thymeleaf, 응답자 = Vanilla JS
- 마이그레이션: Flyway / 로컬 DB: Docker Compose · Testcontainers

## 진입 문서
- 스펙: `docs/AdoptLoop_SPEC.md` — PostgreSQL 스키마, 9 도메인
- 도메인 플로우: `docs/AdoptLoop_DOMAIN_FLOW.md`
- 기술 스택: `docs/AdoptLoop_TECH_STACK.md`
- API: `docs/AdoptLoop_API.yaml` — OpenAPI 3.1
- 구현 계획: `docs/AdoptLoop_PLAN.md` — M1~M9

## 의사결정 기록 (ADR)
- 인덱스: `docs/adr/README.md`
- **새 ADR 작성 트리거**: SPEC 또는 TECH_STACK을 편집해야 하는 결정이라면, 편집 대신 ADR을 추가하고 `Supersedes: ADR-XXXX`로 표시한다.
- 일반 구현 디테일(메서드명, 테스트 도구 선택 등)은 ADR 대상이 아니다.
- 옛 ADR은 **수정/삭제하지 않는다**. 상태만 `Superseded by ADR-YYYY`로 갱신한다.
- 인덱스 갱신: 새 ADR 추가 시 `docs/adr/README.md` 표에 한 줄 append 필수.

## 현재 상태
- 현재 milestone: M8.2 stub 채우기 직전
- 다음 작업: M1 Foundation 구현 시작 (subagent-driven, milestone PR 분리)
- 컷오프 순서 (일정 부족 시): 1순위 Slack 발행 알림, 2순위 관리자 UI → Swagger UI 폴백
- 원격: `git@github.com:FineAppTech/AdoptLoop-server.git` (main 보호 — 모든 작업 feature 브랜치 + PR)
````

- [ ] **Step 2.2: Verify file**

Run:
```bash
test -s docs/PROJECT_CONTEXT.md && echo "context OK"
grep -c "^##" docs/PROJECT_CONTEXT.md
```

Expected:
- `context OK` printed.
- `4` (four `##` section headers: 이 프로젝트는?, 진입 문서, 의사결정 기록, 현재 상태).

- [ ] **Step 2.3: Commit**

```bash
git add docs/PROJECT_CONTEXT.md
git commit -m "$(cat <<'EOF'
docs: PROJECT_CONTEXT.md 추가 — 진입점 + ADR 운영 규칙

새 세션·새 작업자가 처음 보는 문서. 프로젝트 개요, 진입 문서 링크,
ADR 작성/번복 운영 규칙, 현재 milestone 상태를 한 곳에 모음.
다음 단계에서 CLAUDE.md가 이 파일을 @import 한다.
EOF
)"
```

---

## Task 3: CLAUDE.md `@import` 한 줄 추가

**Files:**
- Modify: `CLAUDE.md` (prepend 2 lines at the very top)

- [ ] **Step 3.1: Read the current CLAUDE.md head to confirm the first line**

Run:
```bash
head -3 CLAUDE.md
```

Expected: First non-blank line is `# CLAUDE.md`.

- [ ] **Step 3.2: Insert the import line**

Use the Edit tool to change the very top of `CLAUDE.md`:

Old (first line of file):
```
# CLAUDE.md
```

New (replaces it with import line + blank line + original heading):
```
@docs/PROJECT_CONTEXT.md

# CLAUDE.md
```

Only this one edit. Do not touch any other content.

- [ ] **Step 3.3: Verify**

Run:
```bash
head -5 CLAUDE.md
```

Expected first 3 lines:
```
@docs/PROJECT_CONTEXT.md

# CLAUDE.md
```

- [ ] **Step 3.4: Commit**

```bash
git add CLAUDE.md
git commit -m "$(cat <<'EOF'
docs: CLAUDE.md에 PROJECT_CONTEXT.md @import 추가

behavioral 가이드(기존 본문)는 그대로 두고, 최상단에
@docs/PROJECT_CONTEXT.md 한 줄을 추가해 새 세션마다 프로젝트
오리엔테이션이 자동 로드되도록 한다.
EOF
)"
```

---

## Task 4: ADR-0001, ADR-0002 (스키마·컨벤션)

**Files:**
- Create: `docs/adr/0001-enum-uppercase-convention.md`
- Create: `docs/adr/0002-survey-deadline-required.md`

- [ ] **Step 4.1: Create ADR-0001**

Create `docs/adr/0001-enum-uppercase-convention.md`:

````markdown
# ADR-0001: Enum 표기 전체 UPPERCASE 통일

- 상태: Accepted
- 날짜: 2026-05-26
- 관련: M1 Foundation, `Enums.kt`, DB 스키마, LLM 프롬프트

## Context
초기 SPEC 작성 과정에서 enum 값의 표기 방식이 계층마다 다르게 흘러갈 위험이 있었다.
- Kotlin 코드의 enum은 관습적으로 UPPERCASE.
- DB 컬럼 DEFAULT 값과 CHECK 제약은 소문자/UPPERCASE 어느 쪽이든 가능.
- JSON wire 표현은 Jackson 기본이 enum의 `name()` (UPPERCASE)지만, 클라이언트 친화로 lowercase로 매핑하려는 유혹이 있음.
- LLM 프롬프트에서 enum 예시를 줄 때 케이스가 흔들리면 LLM 출력 파싱 실패 위험.

네 계층(코드/DB/JSON/LLM)이 각자 다른 case로 가면 직렬화·역직렬화·LLM 출력 검증에서 어디가 깨졌는지 추적이 어려워진다.

## Decision
**모든 enum은 UPPERCASE로 통일한다.**
- Kotlin: `enum class Status { DRAFT, PUBLISHED, ... }`
- DB: 컬럼 DEFAULT와 CHECK 제약 모두 UPPERCASE 문자열.
- JSON wire: Jackson 기본 매핑 사용 (`@JsonValue`/`@JsonProperty` 추가 매핑 금지).
- LLM 프롬프트: enum 예시·허용값 목록을 UPPERCASE로 작성.

## Consequences
- ✅ 4개 계층 표기가 완전히 일치 → 직렬화/역직렬화 버그 원천 차단.
- ✅ LLM 출력 파싱이 단순해짐 (정규화 단계 불필요).
- ❌ 응답자/관리자 UI에 노출할 때 display 라벨이 필요 — 화면 측 i18n/매핑으로 처리.
- 추후 외부 SDK가 lowercase enum을 강제하면 그 경계에서만 매핑.
````

- [ ] **Step 4.2: Create ADR-0002**

Create `docs/adr/0002-survey-deadline-required.md`:

````markdown
# ADR-0002: `surveys.deadline`을 NOT NULL로, draft 생성 시점에 입력 강제

- 상태: Accepted
- 날짜: 2026-05-26
- 관련: M1 Foundation, `surveys` 테이블, `SurveyDraftReq`

## Context
초기 SPEC 검토 단계에서 `surveys.deadline`을 nullable로 두는 안이 있었다 ("임시 저장 시점엔 데드라인 미정인 케이스 허용"). 그러나 두 가지 문제가 보였다.
1. **publish 검증 누락 위험**: deadline이 비어 있는 draft가 그대로 publish 가능하면, 응답자에게 "마감 없음" 설문이 노출됨.
2. **분석 시점 cohort 모호**: M6 LLM 분석은 "deadline 기준으로 응답 cohort"를 정의. deadline이 없으면 분석 트리거 시점과 응답 종료 시점의 정합성이 깨짐.

## Decision
- `surveys.deadline TIMESTAMP NOT NULL`.
- `POST /api/admin/surveys/draft` 요청 DTO(`SurveyDraftReq`)에 `deadline` 필수 필드 포함.
- 입력 누락 시 400 `MISSING_DEADLINE`.

## Consequences
- ✅ 스키마 제약으로 invalid state 진입 불가 — 추가 검증 코드 최소화.
- ✅ M3 publish, M6 analyze 모두 deadline 존재를 가정할 수 있음.
- ❌ "데드라인 나중에 정하고 싶다"는 UX 요구가 생기면 별도 컬럼(`deadline_tentative` 등)이나 새 ADR 필요.
````

- [ ] **Step 4.3: Verify**

Run:
```bash
ls docs/adr/000[12]-*.md
wc -l docs/adr/000[12]-*.md
```

Expected: Both files exist with >10 lines each.

- [ ] **Step 4.4: Commit**

```bash
git add docs/adr/0001-enum-uppercase-convention.md docs/adr/0002-survey-deadline-required.md
git commit -m "$(cat <<'EOF'
docs(adr): ADR-0001, ADR-0002 — enum UPPERCASE / deadline NOT NULL

스키마·컨벤션급 결정 두 건 backfill.
- 0001: enum 표기를 코드/DB/JSON/LLM 4개 계층에서 UPPERCASE로 통일.
- 0002: surveys.deadline을 NOT NULL로 하고 draft 생성 시점에 입력 강제.
EOF
)"
```

---

## Task 5: ADR-0003, ADR-0004 (API 컨벤션)

**Files:**
- Create: `docs/adr/0003-jackson-snake-case-and-dto-naming.md`
- Create: `docs/adr/0004-error-mapping-via-response-status-exception.md`

- [ ] **Step 5.1: Create ADR-0003**

Create `docs/adr/0003-jackson-snake-case-and-dto-naming.md`:

````markdown
# ADR-0003: Jackson `SNAKE_CASE` 전역 적용 + DTO 네이밍 컨벤션

- 상태: Accepted
- 날짜: 2026-05-26
- 관련: M2 API 컨벤션, ObjectMapper 설정, 모든 DTO

## Context
- `docs/AdoptLoop_API.yaml`(OpenAPI)는 모든 필드를 `snake_case`로 정의.
- Kotlin DTO는 자연스러운 `camelCase`로 작성됨.
- 두 표기를 잇는 옵션: (a) 필드마다 `@JsonProperty("snake_name")` 부착, (b) ObjectMapper 전역 `PropertyNamingStrategies.SNAKE_CASE` 적용.
- (a)는 필드 수십 개에 어노테이션이 깔리고 누락·오타 발견이 늦음.
- 또한 도메인이 9개로 늘면서 DTO 종류가 혼란스러워질 위험 (요청/응답/도메인VO/내부전달이 한 폴더에 섞임).

## Decision
1. **Jackson 전역 `PropertyNamingStrategies.SNAKE_CASE` 적용** (`@Configuration`에서 ObjectMapper 빈 커스터마이즈).
2. **모든 `@JsonProperty` 어노테이션 제거** — 전역 전략으로 충분.
3. **DTO 네이밍 컨벤션**:
   - `*Req` — 요청 본문 (e.g., `SurveyDraftReq`)
   - `*Res` — 응답 본문 (e.g., `SurveyDraftRes`)
   - `*Vo` — 도메인 값객체 (e.g., `QuestionVo`)
   - `*Dto` — 서비스 계층 내부 전달용 (외부 노출 금지)

## Consequences
- ✅ 어노테이션 보일러플레이트 제거 → 필드 추가 시 누락 위험 0.
- ✅ DTO 의도(입력/출력/내부)를 파일명만으로 분간 가능.
- ❌ 일부 외부 SDK가 camelCase 직렬화를 강제하면 그 경계에서만 별도 ObjectMapper 인스턴스 필요.
- ❌ 한 번의 전역 변경이라 적용 PR 전후로 wire 호환성 점검 필수.
````

- [ ] **Step 5.2: Create ADR-0004**

Create `docs/adr/0004-error-mapping-via-response-status-exception.md`:

````markdown
# ADR-0004: 에러 매핑은 `ResponseStatusException`으로 단일화

- 상태: Accepted
- 날짜: 2026-05-26
- 관련: M2 API 컨벤션, 글로벌 예외 핸들러

## Context
Spring에서 도메인 예외를 HTTP 상태로 변환하는 방식은 두 갈래.
1. `IllegalArgumentException` → 400, `IllegalStateException` → 409 같은 **타입 기반 자동 매핑**을 `@ControllerAdvice`로 일괄 처리.
2. 던지는 지점에서 직접 `ResponseStatusException(HttpStatus.X, "<code>", cause)`를 사용.

(1)은 깔끔해 보이지만, `IllegalArgumentException`이 코드 내 여러 의미로 던져지면(예: 단순 인자 검증 vs 비즈니스 규칙 위반) 상태 코드 의도가 흐려진다. 또한 새 에러 코드 추가 시 핸들러 표가 비대해진다.

## Decision
- **모든 도메인/API 에러는 `ResponseStatusException`(또는 그 wrapper)로 던진다.**
- `IllegalArgumentException`/`IllegalStateException`을 HTTP 상태로 자동 매핑하지 않는다 (글로벌 핸들러에서 제거).
- 에러 응답 본문에는 짧은 `code` 문자열(예: `MISSING_DEADLINE`, `LLM_TRANSIENT`)을 포함.
- 던지는 위치에서 상태 코드와 코드 문자열이 함께 명시되므로 의도가 코드 한 줄에 드러남.

## Consequences
- ✅ "이 에러가 왜 이 상태 코드인가"가 던지는 지점에서 자명.
- ✅ 새 에러 코드 추가 시 핸들러 표 수정 불필요.
- ❌ 던지는 코드가 약간 더 장황 (`throw ResponseStatusException(BAD_REQUEST, "MISSING_DEADLINE")`).
- 추후 i18n 메시지가 필요해지면 wrapper(`AdoptLoopException(code, args)`)로 확장 — 그땐 새 ADR.
````

- [ ] **Step 5.3: Verify**

Run:
```bash
ls docs/adr/000[34]-*.md
grep -l "ResponseStatusException" docs/adr/0004-*.md
grep -l "PropertyNamingStrategies" docs/adr/0003-*.md
```

Expected: All three commands print one filename each (no errors).

- [ ] **Step 5.4: Commit**

```bash
git add docs/adr/0003-jackson-snake-case-and-dto-naming.md docs/adr/0004-error-mapping-via-response-status-exception.md
git commit -m "$(cat <<'EOF'
docs(adr): ADR-0003, ADR-0004 — API 컨벤션 두 건

- 0003: Jackson SNAKE_CASE 전역 적용 + @JsonProperty 제거 + DTO 네이밍(Req/Res/Vo/Dto).
- 0004: ResponseStatusException으로 에러 매핑 단일화, 타입 기반 자동 매핑 제거.
EOF
)"
```

---

## Task 6: ADR-0005 (supersedes 패턴 명시 사례)

**Files:**
- Create: `docs/adr/0005-slack-webhook-as-server-config.md`

- [ ] **Step 6.1: Create ADR-0005**

Create `docs/adr/0005-slack-webhook-as-server-config.md`:

````markdown
# ADR-0005: Slack Webhook URL은 서버 config로 단일 고정

- 상태: Accepted
- 날짜: 2026-05-26
- 관련: M3 Survey publication, `surveys` 테이블, `SurveyPublishReq`(삭제됨)

## Context
초기 SPEC은 `surveys.slack_webhook_url VARCHAR(...)` 컬럼을 두어 설문별 webhook 지정을 허용하려 했다. 발행(publish) API도 본문에 `slack_webhook_url`을 받는 `SurveyPublishReq` DTO를 노출했다.

실제 운영 모델을 재확인한 결과:
- AdoptLoop의 토이 운영자는 1명.
- 모든 설문의 발행 알림이 동일한 채널로 가는 게 자연스러움.
- per-survey webhook은 multi-tenant 시점에야 의미 있음.

이 상태로 두면 (a) 사용 안 하는 컬럼이 스키마에 잔존, (b) publish API가 불필요한 body를 받아 검증·문서화 부담이 늘어남.

## Decision
- `surveys.slack_webhook_url` 컬럼 제거 (Flyway migration).
- `application.yml`에 `adoptloop.slack.webhook-url`(단일 값) 설정. 환경별 override는 Spring profile로.
- `SurveyPublishReq` DTO 삭제. `POST /api/admin/surveys/{id}/publish`는 body 없는 POST.
- 발행 시 서버는 config의 webhook URL을 읽어 알림 전송.

## Consequences
- ✅ 스키마·DTO·API 세 곳 동시 단순화. 변경 한 곳(config) = 운영 변경 한 곳.
- ✅ webhook URL이 DB 평문에 남지 않음 (보안 측면 부수 이득).
- ❌ multi-tenant 또는 "설문별 다른 채널" 요구가 생기면 다시 컬럼화 필요.
- **미래 supersedes 조건**: 위 요구가 발생하면 새 ADR을 작성해 본 ADR을 `Superseded by ADR-XXXX`로 표시. 컬럼·DTO를 다시 도입하는 마이그레이션을 그 ADR에 명시.
````

- [ ] **Step 6.2: Verify**

Run:
```bash
test -s docs/adr/0005-slack-webhook-as-server-config.md && echo "0005 OK"
grep -c "supersedes" docs/adr/0005-slack-webhook-as-server-config.md
```

Expected:
- `0005 OK`.
- `1` (one occurrence of "supersedes" in the Consequences section).

- [ ] **Step 6.3: Commit**

```bash
git add docs/adr/0005-slack-webhook-as-server-config.md
git commit -m "$(cat <<'EOF'
docs(adr): ADR-0005 — Slack Webhook URL을 서버 config로 단일 고정

surveys.slack_webhook_url 컬럼과 SurveyPublishReq DTO를 제거하고
application.yml의 adoptloop.slack.webhook-url로 통합. Consequences에
미래 multi-tenant 전환 시 supersedes될 조건을 명시.
EOF
)"
```

---

## Task 7: ADR-0006 (LLM 트랜잭션 분리)

**Files:**
- Create: `docs/adr/0006-llm-call-outside-transaction-and-503-mapping.md`

- [ ] **Step 7.1: Create ADR-0006**

Create `docs/adr/0006-llm-call-outside-transaction-and-503-mapping.md`:

````markdown
# ADR-0006: LLM 호출은 `@Transactional` 바깥에서 + `LlmTransientException` → 503

- 상태: Accepted
- 날짜: 2026-05-26
- 관련: M4 Draft generation / M6 Analyze, `SurveyDraftService`, `SurveyService.createDraftWithQuestions`, 글로벌 예외 핸들러

## Context
초안 설계는 `SurveyDraftService.createDraft()`에 `@Transactional`을 걸고 그 안에서 Bedrock(Claude Haiku 4.5)을 호출했다. 두 가지 문제가 보였다.

1. **DB 커넥션 점유**: Bedrock 응답은 수 초 단위 지연이 가능. 그 시간 동안 트랜잭션이 열려 있어 커넥션 풀을 점유. 동시 사용자가 늘면 풀 고갈 위험.
2. **장애 분류**: Bedrock의 일시 장애(throttling, 5xx)를 HTTP 4xx로 그대로 노출하면 클라이언트가 retry 정책을 결정할 단서를 잃음.

## Decision
1. **`SurveyDraftService`의 `@Transactional` 제거**. LLM 호출은 트랜잭션 밖에서 실행.
2. **`SurveyService.createDraftWithQuestions(...)`** 메서드 신설 — 이쪽에만 `@Transactional` 부여. LLM 결과를 받아서 survey + questions를 한 트랜잭션에 일괄 저장.
3. **`LlmTransientException`** 신설. Bedrock 일시 장애(throttling, 5xx, timeout)를 이 예외로 wrap.
4. **글로벌 핸들러**: `LlmTransientException` → HTTP 503 + `code: LLM_TRANSIENT`. 영구 장애(API key 오류 등)는 500 그대로.

## Consequences
- ✅ DB 커넥션 점유 시간이 "LLM 호출 시간"이 아니라 "INSERT 몇 건" 시간으로 축소.
- ✅ 클라이언트가 503을 보고 retry 가능 (4xx와 명확히 구분).
- ❌ 부분 실패 시나리오: LLM 성공 → DB 실패. LLM 비용은 이미 발생, 사용자에겐 에러. 토이 규모에선 수용 (재시도하면 새 LLM 호출).
- 추후 idempotency key를 도입해 "LLM 결과 캐시 후 재시도"가 가능해지면 새 ADR로 supersedes.
````

- [ ] **Step 7.2: Verify**

Run:
```bash
test -s docs/adr/0006-llm-call-outside-transaction-and-503-mapping.md && echo "0006 OK"
grep -c "LlmTransientException" docs/adr/0006-*.md
```

Expected: `0006 OK`, count `>= 3`.

- [ ] **Step 7.3: Commit**

```bash
git add docs/adr/0006-llm-call-outside-transaction-and-503-mapping.md
git commit -m "$(cat <<'EOF'
docs(adr): ADR-0006 — LLM 호출 트랜잭션 분리 + 503 매핑

SurveyDraftService의 @Transactional을 제거하고 LLM 호출을 트랜잭션
바깥에서 수행. 결과는 SurveyService.createDraftWithQuestions(트랜잭션)
에서 일괄 저장. Bedrock 일시 장애는 LlmTransientException으로 wrap,
글로벌 핸들러에서 503 LLM_TRANSIENT로 매핑.
EOF
)"
```

---

## Task 8: ADR-0007, ADR-0008 (검증 정책 + 배포)

**Files:**
- Create: `docs/adr/0007-response-validation-policy.md`
- Create: `docs/adr/0008-ecs-deployment-latest-tag-force-new.md`

- [ ] **Step 8.1: Create ADR-0007**

Create `docs/adr/0007-response-validation-policy.md`:

````markdown
# ADR-0007: 응답 검증 정책 — required / cap / 빈 텍스트

- 상태: Accepted
- 날짜: 2026-05-26
- 관련: M5 Response collection, `POST /api/public/surveys/{access_token}/responses`

## Context
응답자 API는 **인증 없는 public 엔드포인트**다. 두 가지 동시 요구.
1. **데이터 품질**: required 질문 누락이나 빈 텍스트가 분석 노이즈로 흘러들어가면 안 됨.
2. **Abuse 1차 방어**: 토이라 정교한 rate limit·CAPTCHA를 두기 어려움. 그러나 무방어면 한 명이 수만 건을 쓰면 DB·LLM 비용이 폭주.

## Decision
- **Required 질문 누락 → 400 `MISSING_REQUIRED`**. 클라이언트 수정으로 해결 가능한 에러.
- **응답 수 cap = `surveys.target_count * 10`**. 초과 시 403 `RESPONSE_CAP_EXCEEDED`. 정상 응답률을 매우 후하게 잡아도 안전 마진.
- **TEXT 응답에서 `trim()` 후 빈 문자열 → 400 `EMPTY_TEXT_RESPONSE`**.

## Consequences
- ✅ Abuse 한 명이 무한히 채우는 시나리오 차단 (cap).
- ✅ M6 분석에 빈 문자열/누락 데이터가 들어오지 않음.
- ❌ cap이 너무 빡빡하면 정상 트래픽도 차단될 수 있음. 운영 데이터를 보고 배수 조정 필요 — 그땐 본 ADR 업데이트 또는 새 ADR.
- ❌ 분산 환경에서 cap 카운트의 race condition 가능 — DB COUNT를 트랜잭션 안에서 잡으면 충분, 정확한 atomic counter는 과함.
````

- [ ] **Step 8.2: Create ADR-0008**

Create `docs/adr/0008-ecs-deployment-latest-tag-force-new.md`:

````markdown
# ADR-0008: ECS 배포는 `:latest` 태그 + `force-new-deployment`

- 상태: Accepted
- 날짜: 2026-05-26
- 관련: M9 Deployment, GitHub Actions, ECS service

## Context
프로덕션 hygiene 관점의 정석은:
- 이미지 태그를 immutable한 값(git SHA, 빌드 번호)으로 고정.
- 새 task definition revision을 만들고 service 업데이트.

이 방식의 장점은 "현재 떠 있는 컨테이너가 어떤 commit인지" 추적이 정확하고, 롤백이 결정적이다.

그러나 AdoptLoop는 7일 토이 프로젝트다. 위 방식은 GitHub Actions 워크플로우 복잡도, task def 관리, IAM 정책을 모두 늘린다. 일정 안에 끝낼 가치를 의도적으로 평가했다.

## Decision
- 이미지는 `:latest` 태그로 ECR에 push (다른 태그 없음).
- 배포는 `aws ecs update-service --force-new-deployment`로 롤아웃.
- 롤백은 "이전 commit을 빌드해서 다시 `:latest`로 push 후 force-new-deployment".

## Consequences
- ✅ 배포 워크플로우가 한 화면에 들어가는 길이로 유지.
- ✅ task def 관리·revision 추적 없음.
- ❌ "지금 떠 있는 게 어느 commit인지"가 ECR 디지스트 + push 시각으로만 추적됨 → 정확한 시점-이미지 대응 어려움.
- ❌ 롤백이 비결정적 (다시 빌드해야 함, 빌드 환경이 바뀌면 결과도 다를 수 있음).
- **이전 가능 조건**: 사용자 1명을 넘어 프로덕션화하면 immutable tag로 즉시 supersedes.
````

- [ ] **Step 8.3: Verify**

Run:
```bash
ls docs/adr/000[78]-*.md
grep -c "^## Decision" docs/adr/0007-*.md docs/adr/0008-*.md
```

Expected: Both files exist. Each shows `1` Decision section.

- [ ] **Step 8.4: Commit**

```bash
git add docs/adr/0007-response-validation-policy.md docs/adr/0008-ecs-deployment-latest-tag-force-new.md
git commit -m "$(cat <<'EOF'
docs(adr): ADR-0007, ADR-0008 — 검증 정책 + ECS 배포 방침

- 0007: 응답 검증 (required 누락 거부, target_count*10 cap, 빈 TEXT 거부).
- 0008: :latest 태그 + force-new-deployment — 토이 우선, production hygiene
  은 의도적으로 후순위. 프로덕션 전환 시 immutable tag로 supersedes 예정.
EOF
)"
```

---

## Task 9: ADR 인덱스(`docs/adr/README.md`) 채우기

**Files:**
- Modify: `docs/adr/README.md`

- [ ] **Step 9.1: Replace the index file with the populated version**

Use the Write tool to overwrite `docs/adr/README.md` with this exact content:

````markdown
# ADR Index

> Architecture Decision Records — AdoptLoop의 SPEC/TECH_STACK 편집급 결정 기록.
> 새 ADR 작성 규칙은 `docs/PROJECT_CONTEXT.md` 참조.

| # | 제목 | 상태 | 한 줄 요약 |
|---|---|---|---|
| [0001](0001-enum-uppercase-convention.md) | Enum UPPERCASE 컨벤션 | Accepted | DB DEFAULT / JSON wire / LLM 프롬프트까지 일관 |
| [0002](0002-survey-deadline-required.md) | Survey deadline NOT NULL | Accepted | draft 생성 시점에 입력 강제 |
| [0003](0003-jackson-snake-case-and-dto-naming.md) | Jackson SNAKE_CASE 전역 + DTO 네이밍 | Accepted | `@JsonProperty` 제거, `Req`/`Res`/`Vo`/`Dto` 컨벤션 |
| [0004](0004-error-mapping-via-response-status-exception.md) | 에러 매핑 `ResponseStatusException` 단일화 | Accepted | `IllegalArgumentException`/`IllegalStateException` 자동 매핑 제거 |
| [0005](0005-slack-webhook-as-server-config.md) | Slack Webhook URL 서버 config 고정 | Accepted | `surveys.slack_webhook_url` 컬럼 제거, `SurveyPublishReq` 삭제 |
| [0006](0006-llm-call-outside-transaction-and-503-mapping.md) | LLM 호출 트랜잭션 분리 + 503 매핑 | Accepted | DB 커넥션 점유 방지, `LlmTransientException` 도입 |
| [0007](0007-response-validation-policy.md) | 응답 검증 정책 | Accepted | required 누락 거부, `target_count*10` cap, 빈 TEXT 거부 |
| [0008](0008-ecs-deployment-latest-tag-force-new.md) | ECS 배포 `:latest` + `force-new-deployment` | Accepted | 토이 우선 — production hygiene 의도적 보류 |
````

- [ ] **Step 9.2: Verify**

Run:
```bash
grep -c "^| \[" docs/adr/README.md
for n in 0001 0002 0003 0004 0005 0006 0007 0008; do
  test -f docs/adr/${n}-*.md && echo "${n} present" || echo "${n} MISSING"
done
```

Expected:
- First command: `8`.
- Loop: 8 lines, all "present".

- [ ] **Step 9.3: Commit**

```bash
git add docs/adr/README.md
git commit -m "$(cat <<'EOF'
docs(adr): README 인덱스에 backfill ADR 0001~0008 등록

8개 backfill ADR을 인덱스 표에 추가. 이후 새 ADR이 추가될 때마다
이 표에 한 줄 append 하는 운영 규칙은 PROJECT_CONTEXT.md에 명시됨.
EOF
)"
```

---

## Task 10: 메모리 파일 갱신 + 최종 검증 + PR

**Files:**
- Modify (outside repo): `/Users/tnear/.claude/projects/-Users-tnear-tnear-toy-projects-adoptloop-server/memory/adoptloop-spec-progress.md`

- [ ] **Step 10.1: Append note to memory file**

Use the Edit tool with these exact old/new strings on `/Users/tnear/.claude/projects/-Users-tnear-tnear-toy-projects-adoptloop-server/memory/adoptloop-spec-progress.md`:

old_string:
```
- 원격 git 연결 완료: `git@github.com:FineAppTech/AdoptLoop-server.git` (실 push는 HTTPS, main 브랜치 보호 룰 있음 → 모든 작업은 feature 브랜치 + PR).
```

new_string:
```
- 원격 git 연결 완료: `git@github.com:FineAppTech/AdoptLoop-server.git` (실 push는 HTTPS, main 브랜치 보호 룰 있음 → 모든 작업은 feature 브랜치 + PR).
- `docs/PROJECT_CONTEXT.md` + `docs/adr/` (0000 template + 0001~0008 backfill + README 인덱스) 시드 완료. `CLAUDE.md`가 `@docs/PROJECT_CONTEXT.md`를 자동 로드. ADR 작성 트리거 = "SPEC/TECH_STACK 편집급 결정만".
```

- [ ] **Step 10.2: Final verification**

Run all these checks:
```bash
# All 10 new doc files present
ls docs/PROJECT_CONTEXT.md docs/adr/README.md docs/adr/0000-template.md docs/adr/000[1-8]-*.md
# CLAUDE.md import line at the very top
head -1 CLAUDE.md
# Branch and commit count
git log --oneline main..HEAD
```

Expected:
- 11 files listed under `docs/` (PROJECT_CONTEXT.md + README.md + 0000-template + 0001~0008).
- First line of `CLAUDE.md`: `@docs/PROJECT_CONTEXT.md`.
- 10 commits ahead of `main`: 1 spec commit (`1f7517c`, from brainstorming) + 9 task commits (Tasks 1–9 each commit once). Verify each commit message corresponds to one logical change.

- [ ] **Step 10.3: Push and open PR**

Run:
```bash
git push -u origin docs/adr-and-project-context-seed
gh pr create --title "docs: ADR + PROJECT_CONTEXT 시드 (0001~0008 backfill)" --body "$(cat <<'EOF'
## Summary
- `docs/PROJECT_CONTEXT.md` 신설 — 새 세션의 진입점 (프로젝트 개요 + ADR 운영 규칙 + 현재 상태).
- `docs/adr/` 신설 — Nygard 클래식 템플릿 + 인덱스 + 핵심 결정 8건 backfill.
- `CLAUDE.md` 최상단에 `@docs/PROJECT_CONTEXT.md` 한 줄 추가 (behavioral 본문은 변경 없음).

## 설계 근거
- 브레인스토밍/설계: `docs/superpowers/specs/2026-05-28-adr-and-project-context-design.md`
- 구현 계획: `docs/superpowers/plans/2026-05-28-adr-and-project-context-seed.md`

## Backfill된 ADR 8건
| # | 제목 |
|---|---|
| 0001 | Enum UPPERCASE 컨벤션 |
| 0002 | Survey deadline NOT NULL |
| 0003 | Jackson SNAKE_CASE 전역 + DTO 네이밍 |
| 0004 | 에러 매핑 ResponseStatusException 단일화 |
| 0005 | Slack Webhook URL 서버 config 고정 (미래 supersedes 조건 명시) |
| 0006 | LLM 호출 트랜잭션 분리 + 503 매핑 |
| 0007 | 응답 검증 정책 |
| 0008 | ECS 배포 :latest + force-new-deployment |

## Test plan
- [ ] `head -1 CLAUDE.md` → `@docs/PROJECT_CONTEXT.md` 확인
- [ ] `docs/adr/README.md` 표의 모든 링크가 깨지지 않음 확인 (수동 클릭 또는 `grep` 검증)
- [ ] 각 ADR 파일이 Nygard 4섹션(Status/Date/Context/Decision/Consequences) 포함 확인
- [ ] 새 세션 시작 시 `PROJECT_CONTEXT.md` 내용이 컨텍스트로 자동 로드되는지 확인
EOF
)"
```

- [ ] **Step 10.4: Final state**

After merge, the next session opening this repo should automatically see:
- `CLAUDE.md` (behavioral guide, unchanged) +
- `docs/PROJECT_CONTEXT.md` (project orientation, auto-imported) +
- ability to navigate `docs/adr/README.md` for past decisions.

When a future change would normally require editing SPEC/TECH_STACK, the workflow becomes: **write a new ADR with `Supersedes: ADR-XXXX`, update the index, mark old ADR's status — do not edit SPEC/TECH_STACK in place.**

---

## Self-Review (run after the plan is fully drafted; this section is for the plan author, not the executor)

Spec coverage check — does every spec requirement map to a task?

| Spec section | Mapped task(s) |
|---|---|
| 5. 파일 구조 | Tasks 1, 2, 3, 4, 5, 6, 7, 8 (every file created) |
| 6.1 CLAUDE.md 변경 | Task 3 |
| 6.2 PROJECT_CONTEXT.md | Task 2 |
| 6.3 ADR README 포맷 | Task 1 (empty), Task 9 (populated) |
| 6.4 ADR 템플릿 | Task 1 |
| 6.5 ADR 0001~0008 내용 | Tasks 4, 5, 6, 7, 8 |
| 7. 운영 원칙 | Embedded in Task 2's PROJECT_CONTEXT content |
| 8. 작업 순서 | Mirrored by task order 1→9 |
| 9. 검증 기준 | Task 10.2 + per-task verify steps |
| 10. 비범위 | Honored — no SPEC/TECH_STACK edits, no M1 implementation |

No placeholders. Type/name consistency (file paths, ADR numbers, DTO suffixes) cross-checked between tasks.
