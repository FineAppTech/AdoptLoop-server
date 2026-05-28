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
