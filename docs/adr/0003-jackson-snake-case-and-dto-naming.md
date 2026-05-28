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
