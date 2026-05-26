# AdoptLoop — 도메인 플로우

> 9장 스키마 × 5장 시나리오 결합. 각 단계마다 어떤 테이블에 어떤 연산이 일어나는지 추적한다.
> 본 문서는 [`AdoptLoop_SPEC.md`](./AdoptLoop_SPEC.md)의 보조 자료다.

**표기 규칙**
- `INSERT/UPDATE/SELECT` = DB 연산
- `→` = 같은 단계 내 후속 효과
- `'..'` = 상태값
- 단계 끝의 `⟪TX⟫` = 트랜잭션 경계

---

## 0. 사전 상태
- `admins`에 관리자 1행이 존재(키 발급 완료).
- 이후 모든 쓰기는 `X-Admin-Key` 인증을 통과한다.

---

## 1-2. 도입 항목 생성 — 시나리오 Step 1-2

```
INSERT adoptions (
  admin_id=:현재관리자, name='Jira 도입', goal, target_audience,
  concern?, target_count, status='active'
)                                                              ⟪TX⟫
```

- 이후 모든 하위 쓰기는 이 `adoption_id`를 통해 admin 소유권을 검증한다.

---

## 3. AI 설문 초안 생성 — 시나리오 Step 3

1. Bedrock 호출 — `adoptions`의 목표/대상자/우려사항을 컨텍스트로 문항 JSON 생성.
2. DB 반영 (한 트랜잭션):

```
INSERT surveys (
  adoption_id, title, public_slug, status='draft'
)                                       -- deadline은 발행 단계에서 채움
INSERT questions[] (
  survey_id, type∈{text,single_choice,scale}, text,
  order_index, required=true, axis(=usage|behavior|value, scale일 때)
)
INSERT question_options[] (
  question_id, text, order_index        -- single_choice 문항에 한해
)                                                              ⟪TX⟫
```

> `public_slug`는 초안 단계에 미리 생성해도 무방(`status='draft'`이라 노출되지 않는다).

---

## 4. 관리자 검토·수정·발행 — 시나리오 Step 4

편집(N회):
```
UPDATE/INSERT/DELETE questions, question_options                ⟪TX⟫
```

발행(1회):
```
UPDATE surveys SET
  status='published', deadline=:dt,
  slack_webhook_url=:url?, published_at=now()                   ⟪TX⟫
```

부수효과(6.7): `slack_webhook_url`이 있으면 단발 POST(제목·마감·`/s/{public_slug}`). 트랜잭션 커밋 후 발사(send-after-commit) — 롤백 시 헛알림 방지.

---

## 5. 응답자 응답 — 시나리오 Step 5

응답자 `/s/{public_slug}` 접속 → 랜딩.

**"설문 시작"** (upfront 생성):
```
INSERT survey_responses (
  survey_id, access_token=:랜덤URL-safe, status='in_progress'
)                                                              ⟪TX⟫
→ 클라이언트가 access_token을 localStorage에 저장
```

**응답 제출** (검증: 토큰 유효 + `surveys.deadline > now()`):
```
INSERT answers[] (
  survey_response_id, question_id,
  text_value | question_option_id | scale_value   -- type에 따라 하나만
)
UPDATE survey_responses
  SET status='submitted', submitted_at=now()                    ⟪TX⟫
```

**마감 전 재방문 수정**: localStorage 토큰 → 본인 응답 로드 →
```
DELETE answers WHERE survey_response_id=:id
INSERT answers[]    -- 전치환 방식(키 충돌 회피, 단순)
-- submitted_at은 최초 제출 시각 유지                          ⟪TX⟫
```

거부 케이스:
- 토큰 없음/잘못됨 → 401
- `deadline` 경과 → 403

---

## 6. 분석 트리거·집계 — 시나리오 Step 6

관리자가 마감된 설문에서 "분석 실행". 사전 검증: `surveys.deadline < now()`.

집계(읽기 전용):
```
SELECT q.id, qo.id, count(*)                  -- single_choice 분포
  FROM answers a JOIN question_options qo ...
SELECT q.id, avg(scale_value)                 -- scale 평균
SELECT count(*) FILTER (WHERE status='submitted')   -- 참여 수
   → 참여율 = / adoptions.target_count
```

주관식(`text_value`) + 위 집계 결과를 LLM 입력으로 구성.

---

## 7-8. AI 분석 저장 — 시나리오 Step 7-8

LLM 1회 호출 → 구조화 JSON: 점수 4종 + 정성 3종 + 액션 제안 N개.

DB 반영:
```
INSERT analyses (
  survey_id,
  adoption_score, usage_score, behavior_score, value_score,
  positive_signals(JSONB), resistance_factors(JSONB), risks(JSONB),
  raw_output(원본 JSON)
)                                                              ⟪TX⟫
```

- **액션 제안은 이 시점에 저장하지 않는다** — 관리자가 채택해야 행이 생긴다. UI 메모리에만 보유(원본은 `raw_output`에서 재구성 가능).
- 재실행: 같은 흐름으로 `analyses` 새 행. "최신=유효"는 `ORDER BY created_at DESC LIMIT 1`.

---

## 9. 액션 아이템 채택·상태 관리 — 시나리오 Step 9

관리자가 제안 목록에서 채택분 선택:
```
INSERT action_items[] (
  adoption_id, analysis_id, title, description?,
  priority∈{high,medium,low}, status='todo'
)                                                              ⟪TX⟫
```

이후 상태 갱신:
```
UPDATE action_items SET status='in_progress' | 'done'
```

비채택은 INSERT 하지 않음으로 표현(별도 상태 불필요).

---

## (후속) 재측정 — MVP 제외

같은 `adoption_id`로 새 `surveys` 행을 만들고 3-8을 반복. 데이터 모델은 이미 지원하며, UI/추이 비교는 SPEC 10장 후속 고려.

---

## 엔티티 생애주기 요약

| 엔티티 | 생성 시점 | 상태 전이 / 종결 |
|---|---|---|
| `admins` | 시스템 부트스트랩/추가 | 키 분실 시 재발급(새 `key_hash`) |
| `adoptions` | Step 1-2 | `active → archived` |
| `surveys` | Step 3 (`draft`) | Step 4 `published`. 응답 거부는 `deadline` 기준 |
| `questions` / `question_options` | Step 3, Step 4 편집 | survey에 종속 |
| `survey_responses` | Step 5 "설문 시작" upfront | `in_progress → submitted` |
| `answers` | Step 5 제출/수정 | `deadline` 전까지 변경 가능 |
| `analyses` | Step 7 (재실행 시 새 행) | 최신 = 유효 |
| `action_items` | Step 9 채택 시 | `todo → in_progress → done` |

---

## 데이터 플로우 다이어그램

```
[admin] ──인증(X-Admin-Key)──► [API]
   │
   ▼  Step 1-2
adoptions ─────────────────────────────────────────────┐
   │                                                   │
   ▼  Step 3 (AI 초안)                                 │
surveys('draft') ─< questions ─< question_options      │
   │                                                   │
   ▼  Step 4 (편집·발행)                               │
surveys('published', deadline)──[Slack POST?]          │
   │                                                   │
   ▼  Step 5 (응답자)                                  │
survey_responses('in_progress')                        │
   │   "설문 시작" → access_token → localStorage       │
   ▼  제출/수정                                         │
answers (text|option|scale)                            │
survey_responses('submitted', submitted_at)            │
   │                                                   │
   ▼  Step 6-7 (관리자 분석 실행 → Bedrock)             │
집계 SELECT + LLM 1회 호출                              │
analyses (점수 4 + 정성 3 + raw_output)                 │
   │                                                   │
   ▼  Step 9 (채택)                                    │
action_items ──────────────────────────────────────────┘
   (adoption_id, analysis_id로 양쪽 추적)
```
