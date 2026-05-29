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

## 개발 규칙
- **작업 워크플로우**: 7단계 절차(보고→수행→셀프검증/리뷰→결과보고→보고서→승인→커밋)·보고서·커밋·네이밍 규칙은 [WORKFLOW.md](WORKFLOW.md) 참조.
- **테스트 강제**: 모든 컨트롤러 테스트는 Spring REST Docs `document()` 호출 필수. JUnit Extension이 미호출 시 fail. → [ADR-0009](adr/0009-spring-restdocs-enforcement.md)

## 현재 상태
- 현재 milestone: M8.2 stub 채우기 직전
- 다음 작업: M1 Foundation 구현 시작 (subagent-driven, milestone PR 분리)
- 컷오프 순서 (일정 부족 시): 1순위 Slack 발행 알림, 2순위 관리자 UI → Swagger UI 폴백
- 원격: `git@github.com:FineAppTech/AdoptLoop-server.git` (main 보호 — 모든 작업 feature 브랜치 + PR)
