# 작업 보고서 — M7 Task 7.1: 관리자 UI(Thymeleaf) + 응답자 페이지

- **Milestone / Task:** M7 (Task 7.1 — UI)
- **브랜치:** `feat/ui` (base: `62ec08e` — PR #10/M6 머지본)
- **날짜:** 2026-05-29
- **성격:** 트리비얼(로직 없음 — view 라우팅 + 정적 템플릿/JS). 인라인 셀프리뷰 + 렌더 스모크 테스트. `/code-review` 미실행.

## 개요

관리자 플로우(도입 목록·생성·상세 → AI 설문 초안 생성 → 문항 편집·발행 → 집계·분석·액션 채택)와 응답자 설문 페이지를 미니멀 UI로 제공. 페이지는 Thymeleaf 템플릿(서버 데이터 바인딩 없음)이고, 동작은 전부 inline/static JS가 기존 JSON API를 `fetch`로 호출한다("디자인보다 동작 우선").

view 컨트롤러(`AdminViewController`/`PublicViewController`)는 REST 엔드포인트가 아니라 ADR-0009(REST Docs 강제) 대상이 아니다. 대신 템플릿 렌더 스모크 테스트로 파싱 오류를 검증한다.

## 변경 파일 목록 (12파일 신규)

| 구분 | 파일 | 핵심 |
|------|------|------|
| 생성 | `web/AdminViewController.kt` | `/admin`·login·adoptions(list/new/detail)·surveys(edit/analyze) 라우팅 → 템플릿명 반환 |
| 생성 | `web/PublicViewController.kt` | `/s/{slug}` → `public/survey` |
| 생성 | `templates/admin/login.html` | Admin Key 입력 → localStorage 저장 (가드 스크립트 미포함) |
| 생성 | `templates/admin/adoptions/list.html` | `GET /api/admin/adoptions` 렌더 |
| 생성 | `templates/admin/adoptions/new.html` | `POST /api/admin/adoptions` |
| 생성 | `templates/admin/adoptions/detail.html` | 도입 상세 + **마감 입력 + 설문 초안 생성**(deadline 본문 전송) |
| 생성 | `templates/admin/surveys/edit.html` | 문항 편집/저장/발행 + 분석 페이지 링크 |
| 생성 | `templates/admin/surveys/analyze.html` | 집계 표시 → 분석 실행 → 제안 액션 채택 |
| 생성 | `templates/public/survey.html` | 응답자 셸(respondent.js 로드) |
| 생성 | `static/css/admin.css` | 공유 미니멀 스타일(각 관리자 페이지가 link) |
| 생성 | `static/js/admin-common.js` | `adminKey()`/`authFetch()`(401→login) + 미로그인 가드 |
| 생성 | `static/js/respondent.js` | 토큰 발급 → 설문 렌더 → 답안 PUT |
| 생성(테스트) | `test/web/WebPagesSmokeTest.kt` | 7개 view 라우트 GET → 200/리다이렉트 + 템플릿 실제 렌더 |

## 검증 (GREEN)

1. **전체 스위트**: `./gradlew test` BUILD SUCCESSFUL — **31 tests / 0 fail / 0 err** (M1~M6 28 + M7 스모크 3). 회귀 0.
2. **렌더 스모크**(`WebPagesSmokeTest`): `webAppContextSetup`으로 GET → Thymeleaf 실제 렌더. 6개 관리자 템플릿 + 공개 템플릿 **파싱 오류 없음**, `/admin`→`/admin/adoptions` 리다이렉트 확인.

### 사전 검증으로 확정한 API 계약 (전역 SNAKE_CASE)

- `GET /adoptions` → `[{id, name, target_count, status, …}]`; `POST /adoptions` 본문 `{name, goal, target_audience, concern?, target_count}` → `{id}`.
- `POST /adoptions/{id}/surveys` 본문 `{deadline}`(@Future) **필수** → `{survey:{id, adoption_id}, questions}`.
- `GET /surveys/{id}` → `{survey:{title, status, deadline, public_slug, adoption_id}, questions:[{id,type,text,axis,options:[{id,text}]}]}`.
- `PUT /surveys/{id}/questions` 본문 `[{type,text,order_index,required,axis,options:[{text,order_index}]}]`.
- `POST /surveys/{id}/publish` → `{public_slug, adoption_id}`; `POST /surveys/{id}/analyses` → `{analysis:{adoption_score,usage_score,behavior_score,value_score,id}, suggested_action_items:[{title,description,priority}]}`.
- `POST /adoptions/{id}/action-items` 본문 `[{analysis_id,title,description,priority}]`.
- 공개: `POST /surveys/{slug}/responses` → `{access_token}`; `GET /responses/{token}` → `{survey:{title,deadline,questions}, answers:[{question_id,text_value,question_option_id,scale_value}]}`; `PUT /responses/{token}/answers` 본문 동일 형태.
- `AdminKeyFilter`: `/api/admin` 외 경로 우회(→ `/admin/*`·`/s/*` 통과), 키 누락/오류 시 **401** → `authFetch`가 login 리다이렉트.

## 결정 / 이탈 사항 (PLAN 대비)

- **PLAN JS 버그 보정 #1 — detail.html 설문 생성**: PLAN은 `POST .../surveys`를 본문 없이 호출 → `SurveyDraftReq{deadline:@Future}` 필수라 400. 페이지에 `datetime-local`(기본 +14일) 추가, 로컬→UTC 변환해 `{deadline}` 전송.
- **이탈 #2 — `layout.html` 생략**: Files 목록의 Thymeleaf fragment는 본문 페이지들이 전혀 참조하지 않음(전부 self-contained). 생성 시 미사용 코드(CLAUDE.md §2)라 **만들지 않음**. 공유 자산은 `admin-common.js`(authFetch·가드) + `admin.css`로 추출해 각 페이지가 link/include.
- **이탈 #3 — `admin-common.js` 신규**: Files 목록엔 없으나 PLAN 본문이 참조. layout의 authFetch/가드를 이 파일로 추출(PLAN 주석 지시와 동일).
- **detail.html `<ul id="surveys">` 제거**: 도입별 설문 목록 조회 API가 없어 채울 수 없는 dead 엘리먼트. 제거. 백엔드 엔드포인트 추가는 M7(UI) 범위 밖이라 하지 않음. 대신 edit.html에 "분석 페이지 →" 링크를 둬 생성→편집→분석 세션 내 이동을 보장. (한계: 발행 후 다른 설문을 다시 열려면 survey id 직접 필요 — 토이 수용.)
- **edit.html 타입 변경 재렌더**: PLAN은 타입 select 변경 시 의존 필드(axis/옵션)를 다시 그리지 않아 빈 화면. `change` 리스너로 재렌더 추가("동작 우선").
- **보강 — `WebPagesSmokeTest`**: PLAN Step 9는 수동검증만 명시. 템플릿 파싱 오류가 주요 런타임 리스크라 헤드리스 렌더 스모크를 추가(비용 저렴, ControllerTestBase의 문서화 강제와 무관하게 IntegrationTestBase 사용).
- ADR 트리거 없음(구현 디테일·정정 범위).

## 디자인 적용 (Pico.css)

미니멀 마크업 위에 시각 디자인을 입혔다. 사용자 선택: **Pico.css(인디고, classless CDN)** + **라이트(중립 배경 + 인디고 포인트)**.

- 7개 템플릿 `<head>`에 `pico.indigo.min.css`(jsDelivr CDN) 링크 + `<html data-theme="light">` 고정. 골격·JS·API 계약은 불변, 시맨틱 HTML을 Pico가 자동 스타일.
- 구조 정리: `<header class="container">` 브랜드 nav + `<main class="container">`, 도입 상세/분석은 `<article>` 카드, 편집 페이지 액션은 `role="group"` 버튼 그룹, 보조 버튼은 `.secondary`/`.outline`.
- `admin.css`는 베이스 규칙을 제거하고 소량 오버라이드(.narrow 로그인 폭, 브랜드 강조, 점수표 열폭, 제안 목록 불릿 제거)만 유지.
- 검증: `./gradlew test` 31 tests GREEN — `WebPagesSmokeTest`가 새 마크업(`<hgroup>`/`<article>`/`role="group"`)까지 실제 렌더해 파싱 오류 없음 확인.
- **다음 작업(다음 주)**: 디자인 폴리시·세부 레이아웃은 이후 작업으로 이월. 현 커밋은 동작 가능한 Pico 베이스라인까지.

## 미수행 (수동 영역)

- PLAN Step 9의 **end-to-end bootRun 수동검증**(devkey 입력 → 도입→설문→발행→응답)은 Docker Postgres + AWS Bedrock 자격증명이 필요해 헤드리스로 실행 불가. 템플릿 렌더 자체는 스모크 테스트로 커버됨.
