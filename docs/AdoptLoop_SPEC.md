# AdoptLoop — 스펙 (확정본)

> 새로운 툴·프로세스·제도를 도입한 뒤 구성원의 익명 피드백을 모아, AI가 설문 설계 → 정착도 분석 → 개선 액션 제안까지 **루프**를 돌려주는 도입 정착 관리 서비스의 MVP 스펙.
>
> 이 문서는 PRD 기반 스펙 초안(`AdoptLoop_SPEC_DRAFT.md`)을 `superpowers:brainstorming`으로 섹션별 발산·수렴하여 확정한 결과물이다. 다음 단계는 `superpowers:writing-plans`로 `AdoptLoop_PLAN.md`를 만드는 것이다.

---

## 1. 한 줄 소개

> 도입은 했는데 잘 쓰이는지 모를 때 — 익명 피드백을 모아 정착도를 측정하고
> 개선 액션으로 이어주는 **AI 기반 도입 정착 피드백 루프**

**핵심 컨셉**

- **용어**: "변화 관리"가 아니라 **"도입 정착(Adoption)"** — 제품명·핵심 시나리오와 일관되고 더 구체적이다.
- **차별점**: 닫힌 **피드백 루프**(수집 → 분석 → 액션 → 재측정)가 핵심 가치다. **AI**는 그 루프를 사람 손 없이 끝까지 돌리는 *엔진*이다 — 경쟁 관계가 아니라 가치(루프)와 수단(AI)의 관계.

---

## 2. 문제 정의

조직은 Jira, n8n, Agile 프로세스, P&C 제도 같은 새로운 도구·방식을 자주 도입한다. 그러나 도입 이후 다음을 지속적으로 추적하지 못한다:

- 구성원이 실제로 잘 사용하고 있는가?
- 어떤 불편을 느끼고 있는가?
- 정착을 위해 무엇을 개선해야 하는가?

**그 결과**: 도입은 했지만 사용률이 낮고, 불만이 누적되어 저항으로 발전하며, 형식적으로만 운영되어 원래 목적과 멀어진다.

| 항목 | 내용 |
|---|---|
| **핵심 고통자** | 도입을 주도·책임지는 **의사결정자(파트 리더 / PO·PM)** — "효과 있었나"라는 질문을 받는 당사자라 절실함·지불 의향이 가장 크다 |
| **근본 원인** | 도구 부재가 아니라 **"루프를 끝까지 돌리는 비용(시간 + 분석 역량)"** — 설계·취합·분석·액션 도출 전 과정이 수작업이라 결국 안/못 한다. AI 엔진이 이 비용을 제거한다 |
| **현행 우회 방식** | ① 일회성 구글폼 설문(수집까지만) ② 1:1 면담·비공식 대화(일화·표본 편향) |
| **진짜 경쟁자** | **일회성 구글폼 설문** — AdoptLoop의 쐐기(wedge)는 "구글폼이 멈추는 수집 지점부터 시작"(분석·액션·재측정) |

---

## 3. 목표

AdoptLoop는 새로운 도입이 조직에 **실제로 정착되고 있는지 측정**하고, **개선 액션까지 이어지는 루프**를 만든다.

### 기능 목표

1. 도입 목표 기반 설문 초안 자동 생성
2. 객관식 / 주관식 응답 수집 (익명)
3. 정착도 점수 및 주요 이슈 분석
4. AI 기반 리스크 / 개선 액션 제안
5. 액션 아이템 상태 추적

### 핵심 정의 및 지표

- **"정착"의 정의 — 3축 복합**:
  ① **사용률** (빈도·범위) ② **행동 정착** (업무 흐름 내재화 깊이 — 형식적 사용과 구분) ③ **인식된 가치** (체감 효용).
  정착도 점수는 세 축으로 구성되며, 각 축은 5점 척도 문항으로 측정한다.
- **성공 지표 (2단)**: 북극성 = 정착도 점수 개선 추이 / 1차 운영 지표 = 액션 아이템 완료율 / 보조 선행지표 = 설문 참여율.
- **측정 구조 — 시계열 추적**: 데이터 모델이 `adoptions` 1:N `surveys`를 지원해 같은 도입에 설문을 여러 번 발행하는 재측정 루프를 구조로 반영한다.
- **MVP 루프 범위 — 1회 사이클 완주**: MVP는 Adoption → 설문 → 응답 → 분석 → 액션 완료까지 한 바퀴를 끝까지 돈다. 재측정·정착도 추이 비교는 데이터 모델만 열어두고 기능·UI는 [후속 고려](#10-후속-고려-mvp-제외)로 분리한다.

---

## 4. 주요 사용자

### 관리자 (제품 사용자)

- **1순위 페르소나**: "도입을 주도·책임지는 의사결정자" — 구체 직군은 **파트 리더 + PO/PM**.
- 프로젝트 리더 / 스크럼 마스터, 조직문화·P&C 담당자도 사용할 수 있으나 의사결정 권한·절실함이 낮아 1순위 페르소나에서는 제외한다.

### 응답자 (사용자가 아닌 데이터 제공자)

- 도입 대상이 되는 구성원 전체. **완전 익명**.

### 사용 행태 결정

- **사용 빈도**: 주기적 사용(대략 주 1회~격주). 새 설문을 매주 만드는 것이 아니라 **진행 중인 정착 루프를 관리**하러 들어온다(응답 확인·분석 검토·액션 상태 갱신). 신규 도입뿐 아니라 기존 도구·방식의 정착 점검도 대상이다. 데일리 도구는 아니다.
- **응답자 동기부여**: 설문 응답을 **회사 정책으로 운영**하는 것을 전제한다 → 제품 차원의 별도 동기 장치(외재적 보상, 응답자용 결과 화면)는 설계하지 않는다.
- **익명성 / 롤 모델**: 롤은 경량으로 둔다 — 관리자 = 개별 키, 응답자 = 응답별 토큰. 별도 `Member` 도메인은 두지 않는다(익명 + 토큰 구조에서 직원을 행으로 저장할 이유가 없다). 응답(`survey_responses`)은 신원과 연결하지 않아 익명이 유지되며, 응답 페이지에 익명 보장 고지 문구를 둔다.
- **응답 수정**: 마감시간 전까지 수정 가능. 응답별 토큰을 브라우저 `localStorage`에 저장해 같은 브라우저로 재방문 시 자기 응답을 수정한다. 토큰은 신원과 연결 저장하지 않는다.

---

## 5. 핵심 사용 시나리오

### 예시: Jira 도입 후 정착도 확인

```
[관리자]
1. "Jira 도입" 항목을 생성
2. 도입 목표, 대상자, 우려사항, 대상 인원수 입력
        ↓
[AI]
3. 객관식/주관식이 섞인 설문 초안 생성
        ↓
[관리자]
4. 문항을 검토·수정하고 마감시간을 정해 설문 발행 (→ 공개 링크 생성, Slack 채널 알림)
        ↓
[응답자]
5. 공개 링크로 들어와 익명으로 응답 제출 (마감 전까지 수정 가능)
        ↓
[관리자]
6. 마감 후 "분석 실행"을 눌러 AI 분석 트리거
        ↓
[시스템 + AI]
7. 객관식 응답 집계 + AI가 주관식·수치 데이터 통합 분석
8. 정착도 점수, 긍정 신호, 저항 요인, 리스크, 액션 아이템 제안
        ↓
[관리자]
9. AI 제안 액션 중 채택할 것을 골라 저장하고 상태 관리
        ↓
(후속 고려 → 다음 사이클로 재측정)
```

### 설계 결정

- **개발 노력 집중점**: 가장 마찰이 큰 단계는 **Step 4(AI 문항 검토)**다 — AI 설문 생성 프롬프트 품질과 관리자 문항 수정 UX에 노력을 집중한다.
- **마감시간**: `surveys`에 마감시간(`deadline`) 필드를 둔다. 발행 시 필수 입력이며, 마감 후 응답을 거부하고 응답 페이지에 표시한다. 관리자의 1회 사이클 능동 조작 시간(참고 추정 ~15–25분)은 설계 목표로 삼지 않는다(검토 품질을 해치므로).
- **AI 분석 트리거**: 관리자 수동 트리거 — 마감된 설문에서 관리자가 "분석 실행"을 눌러 시작한다(스케줄러 불필요). "분석 재실행"도 허용한다.
- **시나리오 분기**: 명시적 분기를 두지 않는다. 응답 부족은 수동 트리거가 관리자 재량으로 흡수하고, AI 이상 결과는 재분석으로 대응한다.
- **Adoption 타입**: 단일 제네릭 모델(타입 enum 없음). 툴·프로세스·제도·정책을 모두 동일 모델로 표현하고, AI는 자유 텍스트 맥락으로 설문을 생성한다. (예: 코드 리뷰 정책, 재택근무 제도, 새 디자인 시스템, n8n 워크플로우 자동화, 애자일 세리머니 변경.)
- **끝까지 추적 (상태 가시화)**: 관리자가 만든 Adoption / Survey / ActionItem이 현재 단계를 **상태값**으로 표현해, 조회 시 "무엇이 어디까지 됐고 무엇을 해야 하는지"가 한눈에 드러나도록 한다.

---

## 6. MVP 기능 범위

### 6.1 도입 항목 관리
- 도입 항목 생성
- 도입 목표, 대상자, 우려사항, **대상 인원수(`target_count`)** 등록
- 도입 상태 관리 (`active` / `archived`)

### 6.2 AI 설문 초안 생성
- 도입 정보 기반 설문 문항 자동 생성 (AI 호출은 AWS Bedrock 경유 — [8장](#8-접근-제어-방식) 참고)
- 문항 타입 **3종**: 주관식 `TEXT` / 단일 선택 `SINGLE_CHOICE` / 5점 척도 `SCALE`
  - `MULTIPLE_CHOICE`는 제외 — 단일 선택 대비 정착도 설문에서 실수요가 작다.
- 관리자가 수정 가능한 설문 초안 저장 (문항 추가·삭제·순서 변경)

### 6.3 설문 발행 및 응답 수집 — "공개 링크 + 응답별 토큰"
- 발행 시 **공개 링크 1개** 생성. 관리자가 슬랙 등 채널에 공유한다.
- 응답 흐름: 직원이 링크 클릭 → 랜딩("설문 시작") → 서버가 **응답별 일회성 토큰** 발급 → 브라우저 `localStorage`에 저장 → 마감 전까지 자기 응답을 수정.
- "설문 시작" 클릭 시 `survey_responses` 행이 즉시(upfront) 생성되고 `access_token`을 보유한다. 토큰은 신원과 연결 저장하지 않아 응답은 익명이다.
- 외부 유출·중복 응답은 하드 차단하지 않는다(`localStorage`로 같은 브라우저 중복만 소프트 방지) — 의식적으로 수용한 MVP 위험.
- 응답 목록 조회

### 6.4 응답 집계
- 객관식 선택지별 응답 수 (`GROUP BY question_option_id`)
- 척도형 문항별 평균 점수
- 응답 수 / **참여율** 조회 — 참여율 = 제출 완료 응답 수 / `adoptions.target_count`

### 6.5 AI 분석 (출력 5종)
- 정착도 점수 (3축: 사용률 / 행동 / 가치 + 종합)
- 주요 긍정 신호 추출
- 주요 저항 요인 추출
- 정착 리스크 분석
- 다음 액션 아이템 제안
- → 점수 4종과 정성 출력 3종은 `analyses` 한 행에, 액션 아이템 제안은 별도 `action_items` 행으로 저장(생애주기를 따로 관리). 한 번의 LLM 호출로 구조화 JSON을 반환한다.

### 6.6 액션 아이템 관리
- AI 제안 액션 중 관리자가 **원하는 것만 골라 저장** (거절은 저장 시점에 일어나므로 기각용 상태 불필요)
- 상태: `TODO` → `IN_PROGRESS` → `DONE`
- 우선순위: `HIGH` / `MEDIUM` / `LOW`

### 6.7 Slack 발행 알림
- `surveys.slack_webhook_url`이 설정돼 있으면, 발행 시 해당 채널에 메시지 1건을 POST한다(설문 제목·마감시간·공개 링크).
- 채널 알림만 — @멘션 타게팅은 하지 않는다(대상자 Slack 핸들 로스터가 필요해지므로).

### MVP에서 제외할 기능
- 로그인 / 회원가입
- 조직 / 팀 복잡한 권한 관리
- Email 알림, Slack @멘션 타게팅
- 실제 Jira / n8n 연동
- 고급 대시보드, 다국어 지원, 결제 / 구독

### 구현 순서 앵커
1순위 = **도입 항목 관리(6.1)** — 루트 도메인이자 가장 단순한 CRUD라 프로젝트 스켈레톤을 확립한다.
2순위 = **AI 설문 생성(6.2)** — 응답 데이터 없이 가능하므로, AWS Bedrock 연동 리스크를 일찍 제거한다.
이후 응답 수집 → 집계 → AI 분석(6.5) → 액션 아이템(6.6) 순.
**컷오프 1순위 후보 = Slack 발행 알림(6.7)** — 핵심 루프는 알림 없이도 완결된다.

---

## 7. 웹 UI

PRD의 선택 옵션 3가지를 검토한 결과:

- **7.1 설문 생성 프롬프트 커스텀 — 제외**: 기본 프롬프트만 사용한다. 관리자는 `Adoption`의 목표·우려사항으로 맥락을 전달한다. 우려사항과 기능이 중복되고 프롬프트 인젝션 표면을 늘리므로 [후속 고려](#10-후속-고려-mvp-제외).
- **7.2 웹 UI — 포함 (응답자 + 관리자 전체)**:
  - 응답자 UI: 랜딩 + 응답/수정 폼 (6.3의 토큰·`localStorage` 흐름상 필수).
  - 관리자 UI: Adoption·Survey·발행·집계·분석·액션 전체 화면.
  - 구현 방식(서버 템플릿 vs SPA)은 기술 스택 결정 시 정한다.
- **7.3 프로젝트 개념 확장(회고 등) — 무시**: `Adoption`을 범용 항목으로 추상화하지 않고 "도입 정착" 시나리오에 특화한다(YAGNI).

> ⚠️ **일정 리스크**: 백엔드 + AI 연동 + 풀 UI(응답자·관리자)를 7일에 담는 것은 빠듯하다. `writing-plans` 단계에서 일정을 재산정하거나 컷오프를 명시해야 한다. 컷오프 순서: ① Slack 발행 알림 ② 관리자 UI는 Swagger UI로 폴백(응답자 UI·핵심 루프는 유지).

---

## 8. 접근 제어 방식

MVP에서는 복잡한 인증/인가 대신 **단순한 접근 제어**를 사용한다.

### 8.1 관리자 인증
- **관리자별 개별 키** 기반 인증. `admins` 엔티티가 키를 보유한다(전역, Adoption별 키 아님).
- 키는 `X-Admin-Key` **헤더로만** 전달한다(쿼리 파라미터는 로그·히스토리 유출 위험으로 제외).
- 키는 DB에 **SHA-256 단방향 해시(`key_hash`)**로 저장한다. 생성 시 평문을 1회만 표시하고, 분실 시 재발급한다.
- 인증 = `SHA-256(들어온 키)`로 인덱싱된 `key_hash`를 직접 조회 → 현재 관리자 해석. 빠른 해시를 쓰는 이유: 키가 고엔트로피 랜덤 문자열이라 무차별 대입이 불가능하고, 해시로 직접 조회해야 하므로(BCrypt 같은 솔티드 해시는 행마다 솔트가 달라 조회 불가).
- 인증은 **단일 인터셉터**에서 "요청 → 현재 관리자 해석"으로 중앙 처리한다. 이 구조 덕에 향후 ID/비밀번호 로그인으로 확장할 때 토큰 리졸버를 *추가*만 하면 된다(기존 변경 없음).

### 8.2 응답자 접근
- 응답자는 **공개 링크 + 응답별 토큰**만 사용한다 — 별도 서비스 키 없음(응답자 클라이언트가 공개라 키에 보안 가치가 없다).
- 공개 링크는 `surveys.public_slug`(비순차 식별자)를 포함한다.
- 응답 단위 통제는 `access_token`이 담당한다. 최소 방어: 유효 토큰이 없으면 거부, 마감시간 후 거부.
- 응답자 식별 정보는 저장하지 않는다(익명).
- **중복 응답 / 링크 유출**은 하드 차단하지 않는다 — 공개 링크는 설계 의도이며, 이는 의식적으로 수용한 MVP 위험이다.

### 8.3 시크릿 관리
- AI(설문 생성·분석)는 **AWS Bedrock**으로 호출한다. 구체 모델은 기술 스택 결정 시 논의한다.
- AWS 자격증명은 배포 환경에서는 **IAM 역할**, 로컬에서는 환경변수 / `~/.aws` 프로파일로 관리한다.
- Slack Webhook URL은 환경변수 또는 `.gitignore`된 로컬 설정으로 관리한다.
- 위 시크릿은 모두 커밋 금지.

---

## 9. 도메인 모델

### 핵심 도메인 (9개)

| 도메인 | 역할 |
|---|---|
| `admins` | 관리자 — 개별 키(`key_hash`) 보유 |
| `adoptions` | 도입 항목 (예: "Jira 도입") |
| `surveys` | 설문 — `adoptions`에 종속 (1:N, 재측정) |
| `questions` | 문항 — `surveys`에 종속 |
| `question_options` | 선택지 — `single_choice` 문항에 종속 |
| `survey_responses` | 익명 응답 1건 — 설문 시작 시 upfront 생성, `access_token` 보유 |
| `answers` | 문항별 답변 — `(survey_response × question)`당 1행 |
| `analyses` | AI 분석 결과 — `surveys`에 종속 |
| `action_items` | 액션 아이템 — `adoptions`에 종속, `analysis_id`로 출처 추적 |

> `Member`는 도입하지 않는다(익명 + 토큰 구조에서 직원을 행으로 저장할 이유가 없다). 응답 토큰은 별도 테이블이 아니라 `survey_responses.access_token` 컬럼으로 둔다(온디맨드 발급이라 토큰과 응답이 동시에 태어난다).

### 관계도 (개념)

```
admins ─< adoptions ─┬─< surveys ─┬─< questions ─< question_options
                     │            ├─< survey_responses ─< answers
                     │            └─< analyses ────────┐
                     └─< action_items ─────────────────┘  (analysis_id = 출처)
```

`answers`는 `survey_responses`에 종속하며 `questions`(어느 문항)·`question_options`(선택된 선택지)도 참조한다.

### 컬럼 설계

> **DB: PostgreSQL.** 모든 식별자는 소문자 `snake_case`, FK 컬럼명 = 참조 테이블 단수형 + `_id`. 모든 PK(`id`)는 `BIGSERIAL`(정수 auto-increment), FK는 `BIGINT`. 시각 컬럼은 `TIMESTAMPTZ`, enum은 `VARCHAR`로 저장(허용값은 비고에 표기), 정성 배열은 `JSONB`.

#### `admins`
| 컬럼 | 타입 | 제약 / 비고 |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `name` | `VARCHAR(100)` | NOT NULL |
| `key_hash` | `VARCHAR(64)` | NOT NULL, UNIQUE — SHA-256 hex(64자) |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, default `now()` |

#### `adoptions`
| 컬럼 | 타입 | 제약 / 비고 |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `admin_id` | `BIGINT` | NOT NULL, FK→`admins(id)` — 생성자 |
| `name` | `VARCHAR(200)` | NOT NULL |
| `goal` | `TEXT` | NOT NULL — 도입 목표 |
| `target_audience` | `TEXT` | NOT NULL — 대상자 설명 |
| `concern` | `TEXT` | nullable — 우려사항 |
| `target_count` | `INTEGER` | NOT NULL — 참여율 분모 |
| `status` | `VARCHAR(20)` | NOT NULL, default `'active'` — `active`/`archived` |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, default `now()` |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL |

#### `surveys`
| 컬럼 | 타입 | 제약 / 비고 |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `adoption_id` | `BIGINT` | NOT NULL, FK→`adoptions(id)` |
| `title` | `VARCHAR(200)` | NOT NULL |
| `public_slug` | `VARCHAR(64)` | NOT NULL, UNIQUE — 공개 링크용 비순차 식별자 |
| `deadline` | `TIMESTAMPTZ` | NOT NULL — 마감시간 |
| `slack_webhook_url` | `TEXT` | nullable — 발행 알림 채널 |
| `status` | `VARCHAR(20)` | NOT NULL, default `'draft'` — `draft`/`published`/`closed` |
| `published_at` | `TIMESTAMPTZ` | nullable |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, default `now()` |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL |

#### `questions`
| 컬럼 | 타입 | 제약 / 비고 |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `survey_id` | `BIGINT` | NOT NULL, FK→`surveys(id)` |
| `type` | `VARCHAR(20)` | NOT NULL — `text`/`single_choice`/`scale` |
| `text` | `TEXT` | NOT NULL — 문항 내용 |
| `order_index` | `INTEGER` | NOT NULL — 문항 순서 |
| `required` | `BOOLEAN` | NOT NULL, default `true` |
| `axis` | `VARCHAR(20)` | nullable — `usage`/`behavior`/`value` (`scale` 문항만) |

#### `question_options`
| 컬럼 | 타입 | 제약 / 비고 |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `question_id` | `BIGINT` | NOT NULL, FK→`questions(id)` — `single_choice` 문항만 |
| `text` | `VARCHAR(300)` | NOT NULL — 선택지 내용 |
| `order_index` | `INTEGER` | NOT NULL |

#### `survey_responses`
| 컬럼 | 타입 | 제약 / 비고 |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `survey_id` | `BIGINT` | NOT NULL, FK→`surveys(id)` |
| `access_token` | `VARCHAR(64)` | NOT NULL, UNIQUE — 응답자 재접근 키(`localStorage` 저장) |
| `status` | `VARCHAR(20)` | NOT NULL, default `'in_progress'` — `in_progress`/`submitted` |
| `submitted_at` | `TIMESTAMPTZ` | nullable — 최초 제출 시각 |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, default `now()` — =설문 시작 시각 |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL |

> 신원 컬럼 없음 — 익명.

#### `answers`
| 컬럼 | 타입 | 제약 / 비고 |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `survey_response_id` | `BIGINT` | NOT NULL, FK→`survey_responses(id)` |
| `question_id` | `BIGINT` | NOT NULL, FK→`questions(id)` |
| `text_value` | `TEXT` | nullable — `type=text`일 때 |
| `question_option_id` | `BIGINT` | nullable, FK→`question_options(id)` — `type=single_choice`일 때 |
| `scale_value` | `INTEGER` | nullable — `type=scale`일 때 (1~5) |

> 문항 타입에 따라 `text_value`/`question_option_id`/`scale_value` 중 하나만 채운다.

#### `analyses`
| 컬럼 | 타입 | 제약 / 비고 |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `survey_id` | `BIGINT` | NOT NULL, FK→`surveys(id)` |
| `adoption_score` | `INTEGER` | NOT NULL — 정착도 종합 점수 |
| `usage_score` | `INTEGER` | NOT NULL — 사용률 축 |
| `behavior_score` | `INTEGER` | NOT NULL — 행동 정착 축 |
| `value_score` | `INTEGER` | NOT NULL — 인식된 가치 축 |
| `positive_signals` | `JSONB` | NOT NULL — 긍정 신호(문자열 배열) |
| `resistance_factors` | `JSONB` | NOT NULL — 저항 요인(문자열 배열) |
| `risks` | `JSONB` | NOT NULL — 정착 리스크(문자열 배열) |
| `raw_output` | `TEXT` | NOT NULL — AI 원본 응답 |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, default `now()` — 재분석 시 새 행, 최신이 유효 |

#### `action_items`
| 컬럼 | 타입 | 제약 / 비고 |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `adoption_id` | `BIGINT` | NOT NULL, FK→`adoptions(id)` |
| `analysis_id` | `BIGINT` | NOT NULL, FK→`analyses(id)` — 출처 분석 |
| `title` | `VARCHAR(300)` | NOT NULL |
| `description` | `TEXT` | nullable |
| `priority` | `VARCHAR(10)` | NOT NULL — `high`/`medium`/`low` |
| `status` | `VARCHAR(20)` | NOT NULL, default `'todo'` — `todo`/`in_progress`/`done` |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, default `now()` |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL |

### 모델 설계 결정

- **출처 추적**: `action_items.analysis_id`로 액션이 어느 분석 제안에서 나왔는지 기록한다 — 재측정 시 "그 액션 후 점수가 올랐나"를 데이터로 잇기 위함.
- **익명 매핑**: 익명성은 `survey_responses` 레벨에서 보장된다(신원 컬럼 없음). `answers`가 `(응답 × 문항)`당 1행이므로 문항 개수·타입 조합이 설문마다 달라도 스키마는 불변이다.
- **`question_options` 별도 테이블**: `answers.question_option_id` FK 무결성과 선택지별 집계(`GROUP BY`)에 필요하다 — `questions` 내 JSON으로 합치지 않는다.
- **가장 취약한 모델 = `analyses`**: AI 출력 구조는 진화하기 쉽다. 방어책 — 점수류는 숫자 컬럼, 정성 출력(`positive_signals`/`resistance_factors`/`risks`)은 **JSON 배열**, AI 원본 응답은 `raw_output`에 보존. UI는 정형 컬럼만 1급으로 렌더하고 `raw_output`은 접이식 "원본 보기"(2급)로 둔다. "원본의 특정 값을 1급으로 보여주고 싶으면 그 필드를 정형 컬럼으로 승격한다"가 규칙이다.
- **`questions.axis`**: AI가 설문 생성 시 `scale` 문항을 정착도 축으로 태깅하면, 분석 시 축 판단을 돕고 `analyses`의 3축 점수 산출 근거가 된다.

---

## 10. 후속 고려 (MVP 제외)

MVP 범위를 벗어나 명시적으로 보류한 항목:

- **재측정 사이클 UI** — 설문 재발행, 정착도 점수 추이 비교 화면 (데이터 모델은 이미 지원).
- **알림 확장** — Email 알림, Slack @멘션 타게팅 (관리자 이탈 방지의 진짜 해법).
- **관리자 로그인 / 회원가입** — ID/비밀번호 인증 (8.1 구조가 확장 안전하게 설계됨).
- **7.1 설문 생성 프롬프트 커스텀**.
- **7.3 프로젝트 회고·스프린트 회고 등으로의 개념 확장**.
- **마감시간 연장** 기능.
- **조회 전용 결과 공유 링크**.
- **익명성 강화** — 집계 임계값(min N), 중복 응답 하드 차단.
- **운영 정리** — `in_progress`로 방치된 `survey_responses` 정리.

---

## 11. 알려진 리스크

- **일정 리스크**: 백엔드 + AWS Bedrock 연동 + 풀 UI(응답자·관리자)를 7일 토이 일정에 담는 것은 빠듯하다. 컷오프 순서 — ① Slack 발행 알림 ② 관리자 UI는 Swagger UI 폴백.
- **`analyses` 모델 취약성**: AI 출력 구조 진화로 가장 먼저 깨질 모델. JSON 배열 + `raw_output` 보존으로 충격을 흡수한다(9장 참고).
- **익명성의 잔여 한계**: 주관식(`TEXT`) 답변 내용에 자기식별 문구가 들어가면 내용으로 식별될 수 있다 — 구조로는 막을 수 없고, 응답 페이지 익명 고지 문구로 완화한다.
- **응답 무결성**: 공개 링크라 외부인 응답·다른 브라우저 중복 응답을 하드 차단하지 못한다 — 의식적으로 수용한 MVP 위험.

---

## 변경 이력

| 날짜 | 내용 |
|---|---|
| 2026-05-22 | `AdoptLoop_SPEC_DRAFT.md`의 9개 섹션 🔄 브레인스토밍 포인트를 모두 발산·수렴하여 확정본으로 격상. 주요 결정: 도입 정착 용어 채택, 3축 정착 정의, 시계열 측정 구조, 공개 링크+응답별 토큰 모델, 관리자별 개별 키(SHA-256), AI는 AWS Bedrock 경유, 웹 UI 전체 구현, 9개 도메인 확정 |
| 2026-05-22 | 검토 반영 — DB는 PostgreSQL 확정, 모든 PK는 `BIGSERIAL`(정수 auto-increment), 9장 스키마를 PostgreSQL 타입 기준 테이블로 재정리. 용어 통일(응답률→참여율) |
