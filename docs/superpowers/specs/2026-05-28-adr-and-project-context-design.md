# ADR + PROJECT_CONTEXT 도입 설계

- 작성일: 2026-05-28
- 작성자: ch.ha@tnear.com (브레인스토밍: Claude Opus 4.7)
- 상태: Approved
- 영향 범위: `CLAUDE.md`, `docs/PROJECT_CONTEXT.md` (신규), `docs/adr/**` (신규)

## 1. 배경 (왜 도입하나)

AdoptLoop는 brainstorming → SPEC → TECH_STACK → API → PLAN 워크플로우로 초기 설계까지 완료했다. 이 문서들은 "처음 짓는 사람" 관점에서는 충분하지만, **구현 이후 운영 단계**(버그 수정, 기능 추가, 결정 번복)에서는 두 가지 정보가 비어 있다:

1. **왜 그 결정을 했는가** — SPEC/TECH_STACK은 "현재 상태"만 보여줌. "이전엔 X였는데 Y로 바꿨고 그 이유는…" 같은 결정 이력이 없으면 6개월 뒤 자기 자신도 PR을 보고 헤맴.
2. **새 세션/새 사람의 진입점** — 현재 `CLAUDE.md`는 프로젝트 무관한 behavioral 가이드만 담고 있어, "이 프로젝트가 뭐고 지금 어디까지 왔는지"를 알려주지 않음.

이를 채우기 위해 **ADR (Architecture Decision Records) + PROJECT_CONTEXT.md** 두 가지를 초기부터 시드한다.

## 2. 목표

- 결정 이력을 잃지 않는다. 특히 **번복된 결정**(supersedes)을 추적 가능하게 한다.
- 새 세션이 시작될 때 진입점을 단일화한다 (`CLAUDE.md` → `PROJECT_CONTEXT.md` → 필요 시 ADR).
- 운영 단계에서 "관련 ADR만 선택적으로 로드"하는 워크플로우를 가능하게 한다.
- 7일 토이 프로젝트 규모에 맞게 **가볍게** 시드한다 (over-engineering 금지).

## 3. 비목표

- TECH_STACK의 모든 결정을 ADR로 전환하지 않는다 (Q1에서 옵션 C 기각).
- 다른 ADR 도구(adr-tools CLI 등)를 도입하지 않는다 — 평문 마크다운으로 충분.
- CHANGELOG는 만들지 않는다 — git/PR 히스토리로 대체.

## 4. 의사결정 (브레인스토밍 합의)

| Q | 결정 | 이유 |
|---|---|---|
| Q1: backfill 범위 | **B** — 핵심 결정 5~8개만 backfill | PLAN 리뷰에서 나온 "번복 결정"이 정확히 ADR 적합. 전체 backfill은 토이에 과함. |
| Q2: CLAUDE.md 구조 | **B** — CLAUDE.md(behavioral) + `docs/PROJECT_CONTEXT.md`(프로젝트 컨텍스트). CLAUDE.md 상단에 `@docs/PROJECT_CONTEXT.md` import 1줄. | behavioral 가이드는 다른 프로젝트에서도 재사용. 프로젝트 컨텍스트와 수명주기가 다름. |
| Q3: ADR 포맷 | **A** — Nygard 클래식 (Title / Status / Context / Decision / Consequences), 본문 한국어. | 업계 표준, 기존 docs와 언어 통일, 토이엔 MADR이 과함. |
| Q4: 폴더/번호 | **A** — `docs/adr/` flat, sequential zero-padded (`0001-*.md`). + `README.md` 인덱스 + `0000-template.md`. | `supersedes ADR-0003` 상호 참조가 깔끔. 토이 규모에 카테고리 분리 불필요. |
| Q5: ADR 작성 트리거 | **A** — "SPEC/TECH_STACK을 편집해야 하는 결정"만 ADR. 일반 구현 디테일은 제외. | 객관적 기준, 단일 규칙으로 운영. PROJECT_CONTEXT에 한 줄로 박아둠. |
| Q6: backfill 대상 수 | **C** — 8개 전부 (1~8) | PLAN 리뷰에서 나온 결정 중 SPEC/TECH_STACK 편집급 모두 기록. 검증 정책(7)·배포 방침(8)도 명시적 트레이드오프라 기록 가치 있음. |

## 5. 최종 파일 구조

```
adoptloop-server/
├── CLAUDE.md                                                    # [변경] 최상단 @import 1줄 추가
├── docs/
│   ├── PROJECT_CONTEXT.md                                        # [신규]
│   ├── AdoptLoop_SPEC.md                                         # (기존)
│   ├── AdoptLoop_DOMAIN_FLOW.md                                  # (기존)
│   ├── AdoptLoop_TECH_STACK.md                                   # (기존)
│   ├── AdoptLoop_API.yaml                                        # (기존)
│   ├── AdoptLoop_PLAN.md                                         # (기존)
│   └── adr/
│       ├── README.md                                             # [신규] ADR 인덱스
│       ├── 0000-template.md                                      # [신규] 복사용 빈 템플릿
│       ├── 0001-enum-uppercase-convention.md                     # [신규]
│       ├── 0002-survey-deadline-required.md                      # [신규]
│       ├── 0003-jackson-snake-case-and-dto-naming.md             # [신규]
│       ├── 0004-error-mapping-via-response-status-exception.md   # [신규]
│       ├── 0005-slack-webhook-as-server-config.md                # [신규]
│       ├── 0006-llm-call-outside-transaction-and-503-mapping.md  # [신규]
│       ├── 0007-response-validation-policy.md                    # [신규]
│       └── 0008-ecs-deployment-latest-tag-force-new.md           # [신규]
```

## 6. 파일별 상세

### 6.1 `CLAUDE.md` 변경

기존 파일 최상단(첫 줄)에 다음 한 줄을 추가하고 빈 줄 하나로 분리:

```markdown
@docs/PROJECT_CONTEXT.md

# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

(이하 기존 내용 그대로)
```

본문은 **수정하지 않는다**.

### 6.2 `docs/PROJECT_CONTEXT.md` 골격

```markdown
# AdoptLoop — Project Context

## 이 프로젝트는?
AdoptLoop는 설문 작성·발행·응답 수집·LLM 분석까지 자동화하는 7일 토이 프로젝트.
Spring Boot 4 + Kotlin + PostgreSQL + Bedrock(Claude Haiku 4.5).
관리자는 Thymeleaf, 응답자는 Vanilla JS.

## 진입 문서
- 스펙: `docs/AdoptLoop_SPEC.md` (PostgreSQL 스키마, 9 도메인)
- 도메인 플로우: `docs/AdoptLoop_DOMAIN_FLOW.md`
- 기술 스택: `docs/AdoptLoop_TECH_STACK.md`
- API: `docs/AdoptLoop_API.yaml` (OpenAPI 3.1)
- 구현 계획: `docs/AdoptLoop_PLAN.md` (M1~M9)

## 의사결정 기록 (ADR)
- 인덱스: `docs/adr/README.md`
- **새 ADR 작성 트리거**: SPEC 또는 TECH_STACK을 편집해야 하는 결정이라면, 편집 대신 ADR을 추가하고 `Supersedes: ADR-XXXX`로 표시한다.
- 일반 구현 디테일(메서드명, 테스트 도구 선택 등)은 ADR 대상이 아니다.
- 옛 ADR은 **수정/삭제하지 않는다**. 상태만 `Superseded by ADR-YYYY`로 갱신한다.

## 현재 상태
- 현재 milestone: M8.2 stub 채우기 직전
- 다음 작업: M1 Foundation 구현 시작 (subagent-driven, milestone PR 분리)
- 컷오프 순서 (일정 부족 시): 1순위 Slack 발행 알림, 2순위 관리자 UI → Swagger UI 폴백
```

(현재 상태 섹션은 milestone 진행에 따라 갱신.)

### 6.3 `docs/adr/README.md` 인덱스 포맷

```markdown
# ADR Index

> Architecture Decision Records — AdoptLoop의 SPEC/TECH_STACK 편집급 결정 기록.
> 새 ADR 작성 규칙은 `docs/PROJECT_CONTEXT.md` 참조.

| # | 제목 | 상태 | 한 줄 요약 |
|---|---|---|---|
| [0001](0001-enum-uppercase-convention.md) | Enum UPPERCASE 컨벤션 | Accepted | DB DEFAULT/JSON/LLM 프롬프트까지 일관 |
| [0002](0002-survey-deadline-required.md) | Survey deadline NOT NULL | Accepted | draft 생성 시점에 입력 강제 |
| [0003](0003-jackson-snake-case-and-dto-naming.md) | Jackson SNAKE_CASE 전역 + DTO 네이밍 | Accepted | `@JsonProperty` 제거, Req/Res/Vo/Dto 컨벤션 |
| [0004](0004-error-mapping-via-response-status-exception.md) | 에러 매핑 ResponseStatusException 단일화 | Accepted | IllegalArgumentException/IllegalStateException 매핑 제거 |
| [0005](0005-slack-webhook-as-server-config.md) | Slack Webhook URL 서버 config 고정 | Accepted | `surveys.slack_webhook_url` 컬럼 제거, `SurveyPublishReq` 삭제 |
| [0006](0006-llm-call-outside-transaction-and-503-mapping.md) | LLM 호출 트랜잭션 분리 + 503 매핑 | Accepted | DB 커넥션 점유 방지, `LlmTransientException` 도입 |
| [0007](0007-response-validation-policy.md) | 응답 검증 정책 | Accepted | required 누락 거부, target_count*10 cap, TEXT 빈 문자열 거부 |
| [0008](0008-ecs-deployment-latest-tag-force-new.md) | ECS 배포 `:latest` + `force-new-deployment` | Accepted | 토이 우선 — production hygiene 의도적으로 보류 |
```

### 6.4 `docs/adr/0000-template.md`

```markdown
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
```

### 6.5 Backfill ADR 8개 — 각 ADR의 핵심 내용

각 ADR은 Nygard 4섹션을 채운다. PLAN 리뷰 결정의 "Why"는 메모리에 보존되어 있는 합의 내용을 옮긴다.

**ADR-0001: Enum UPPERCASE 컨벤션**
- Context: SPEC 초안엔 enum 표기가 일관되지 않았고, DB DEFAULT/JSON wire/LLM 프롬프트가 각자 다른 case로 가면 직렬화·역직렬화·LLM 출력 파싱이 깨질 위험.
- Decision: 모든 enum을 UPPERCASE로 통일. `Enums.kt` + DB DEFAULT + JSON wire (`@JsonValue` 또는 Jackson 기본 매핑) + LLM 프롬프트 예시 모두 동일.
- Consequences: ✅ 4개 계층 일관성. ❌ 응답자 UI에서 노출 시 display 라벨 별도 매핑 필요.

**ADR-0002: Survey deadline NOT NULL + draft 시점 입력**
- Context: 초기 SPEC은 deadline을 nullable로 두려 했으나, "draft만 만들고 deadline 비워두면 publish 시 검증 누락 가능성" + "LLM 분석/통계 시점에 deadline이 없으면 cohort 정의가 모호".
- Decision: `surveys.deadline NOT NULL` + draft 생성 API에서 입력 강제.
- Consequences: ✅ 스키마 단순화, 검증 코드 감소. ❌ 사용자가 "임시 저장" 욕구가 있을 경우 UX 보완 필요.

**ADR-0003: Jackson SNAKE_CASE 전역 + DTO 네이밍 컨벤션**
- Context: API.yaml은 snake_case, Kotlin DTO는 camelCase. `@JsonProperty`를 필드마다 붙이는 방식은 누락·오타 위험.
- Decision: Jackson `PropertyNamingStrategies.SNAKE_CASE`를 전역 적용. 모든 `@JsonProperty` 제거. DTO 네이밍은 `*Req` (요청) / `*Res` (응답) / `*Vo` (도메인 값객체) / `*Dto` (내부 전달용)로 통일.
- Consequences: ✅ 보일러플레이트 감소, 누락 위험 제로. ❌ 일부 외부 SDK가 camelCase를 요구하면 별도 ObjectMapper 인스턴스 필요.

**ADR-0004: 에러 매핑 ResponseStatusException 단일화**
- Context: Spring 관습으로 `IllegalArgumentException` → 400, `IllegalStateException` → 409 같은 자동 매핑을 쓸 수 있지만, 의미가 호출 위치마다 달라 매핑이 흐려짐.
- Decision: 모든 도메인 에러는 `ResponseStatusException(HttpStatus.XXX, "<code>", cause)` 또는 그 wrapper로 던진다. 자동 예외→상태 매핑 제거.
- Consequences: ✅ 상태 코드와 메시지가 던지는 지점에서 명시적. ❌ 던질 때 코드 약간 더 장황.

**ADR-0005: Slack Webhook URL 서버 config 고정** ⭐ 미래 supersedes 조건을 명시한 사례
- Context: 초기 SPEC은 `surveys.slack_webhook_url` 컬럼을 두어 survey별 webhook을 허용. 실제 토이 운영자는 1명, webhook도 하나만 사용 예정.
- Decision: 컬럼 제거. `application.yml`의 `adoptloop.slack.webhook-url`로 단일 고정. `SurveyPublishReq` DTO 삭제 (publish는 body 없는 POST).
- Consequences: ✅ 스키마 단순화, DTO 1개 감소, 운영 한 곳 변경. ❌ multi-tenant 전환 시 다시 컬럼화 필요 → 그땐 새 ADR로 supersedes.

**ADR-0006: LLM 호출 트랜잭션 분리 + `LlmTransientException` → 503 매핑**
- Context: 초안 `SurveyDraftService`가 `@Transactional` 안에서 Bedrock 호출 → LLM 응답이 수 초 지연되면 DB 커넥션이 그동안 점유. 또한 Bedrock 일시 장애는 5xx로 노출돼야 클라이언트가 retry 가능.
- Decision: `SurveyDraftService`의 `@Transactional` 제거. LLM 호출은 트랜잭션 밖에서 실행 후, 결과를 받아 `SurveyService.createDraftWithQuestions`(트랜잭션 메서드)에서 일괄 저장. `LlmTransientException` 신설 + 글로벌 핸들러에서 `503 LLM_TRANSIENT`로 매핑.
- Consequences: ✅ DB 커넥션 점유 시간 최소화, 클라이언트 retry 안내 가능. ❌ 부분 실패 시(LLM 성공 + DB 실패) LLM 비용 발생 후 폐기 — 토이 규모에선 수용.

**ADR-0007: 응답 검증 정책**
- Context: 응답자 API(`POST /responses`)는 인증이 없는 public 엔드포인트 — abuse 대응 + 데이터 품질 둘 다 필요.
- Decision:
  - required 질문 누락 → 400.
  - 동일 survey의 응답 수 cap = `target_count * 10`. 초과 시 403.
  - TEXT 응답에서 trim 후 빈 문자열 → 400.
- Consequences: ✅ abuse 1차 방어, 분석 노이즈 감소. ❌ cap이 너무 빡빡하면 정상 트래픽도 차단 — 추후 운영 데이터 보고 조정.

**ADR-0008: ECS 배포 `:latest` 태그 + `force-new-deployment`**
- Context: 프로덕션 hygiene 관점에선 이미지 immutable tag (예: git SHA) + task definition revision 갱신이 정석이지만, 7일 토이엔 운영 부담이 크다.
- Decision: 이미지는 `:latest` 태그로 push, `aws ecs update-service --force-new-deployment`로 롤아웃. 롤백은 이전 이미지 재push.
- Consequences: ✅ 배포 파이프라인 단순. ❌ 정확한 시점-이미지 대응이 어렵고 롤백 비결정적 — 프로덕션 전환 시 immutable tag으로 supersedes 필요.

## 7. 운영 원칙 (이후 모든 결정에 적용)

1. **트리거**: SPEC 또는 TECH_STACK을 편집해야 하는 결정이라면 → 편집 대신 ADR을 추가한다.
2. **번복 시**: 새 ADR을 작성하고 헤더에 `Supersedes: ADR-XXXX`. 옛 ADR은 상태만 `Superseded by ADR-YYYY`로 변경 — 본문은 건드리지 않는다.
3. **인덱스 갱신**: 새 ADR 추가 시 `docs/adr/README.md` 표에 한 줄 추가 필수.
4. **번호**: 마지막 ADR 번호 + 1. zero-padded 4자리.
5. **상태 전이**: `Proposed → Accepted → (Superseded | Deprecated)`. Proposed 단계는 PR 리뷰 중 사용.

## 8. 작업 순서 (구현 단계)

1. `docs/adr/` 폴더 생성 + `0000-template.md` + `README.md` (인덱스, 빈 표) 작성.
2. `docs/PROJECT_CONTEXT.md` 작성 (위 6.2 골격대로).
3. `CLAUDE.md` 최상단에 `@docs/PROJECT_CONTEXT.md` 한 줄 추가.
4. ADR 0001~0008 작성. 작은 것(0001, 0002)부터 시작해서 supersedes 패턴이 있는 0005를 중간에.
5. `docs/adr/README.md` 인덱스 표를 ADR 8개로 채움.
6. 메모리 파일(`adoptloop-spec-progress.md`)에 "ADR/PROJECT_CONTEXT 시드 완료" 항목 추가.
7. 단일 PR로 묶어 main에 머지 (main 보호 룰).

## 9. 검증 기준

- `docs/PROJECT_CONTEXT.md`가 Claude Code 세션에서 자동 로드된다 (CLAUDE.md의 `@import` 동작 확인).
- `docs/adr/README.md`의 모든 링크가 깨지지 않는다.
- 각 ADR 파일이 Nygard 4섹션(Context/Decision/Consequences + 헤더 메타)을 모두 포함한다.
- ADR-0005의 Consequences에 "multi-tenant 전환 시 새 ADR로 supersedes" 조건이 명시되어, supersedes 패턴이 실제로 어떻게 트리거될지 독자가 이해할 수 있다.

## 10. 비범위 (이번 작업에서 하지 않는 것)

- SPEC/TECH_STACK 본문 수정 (ADR이 supersedes하므로 그대로 둠).
- M1 Foundation 구현 시작 (별도 작업).
- CHANGELOG, RFC 디렉토리 등 추가 운영 문서 도입.
