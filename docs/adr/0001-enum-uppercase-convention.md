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
