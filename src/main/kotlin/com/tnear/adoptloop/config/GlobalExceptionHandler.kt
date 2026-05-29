package com.tnear.adoptloop.config

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {

    data class ErrorRes(val code: String, val message: String)

    @ExceptionHandler(NoSuchElementException::class)
    fun notFound(e: NoSuchElementException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorRes("NOT_FOUND", e.message ?: ""))

    @ExceptionHandler(ResponseStatusException::class)
    fun responseStatus(e: ResponseStatusException) =
        ResponseEntity.status(e.statusCode).body(ErrorRes(
            (e.statusCode as? HttpStatus)?.name ?: e.statusCode.value().toString(),
            e.reason ?: ""))

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
// 위 responseStatus 핸들러가 status는 유지하고 응답 바디를 ErrorRes 포맷으로 통일한다.
