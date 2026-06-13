package com.spartan.bolao.web

import com.spartan.bolao.security.GoogleAuthException
import com.spartan.bolao.security.GoogleNotConfiguredException
import com.spartan.bolao.security.UnauthenticatedException
import com.spartan.bolao.service.EmailAlreadyUsedException
import com.spartan.bolao.service.InvalidCredentialsException
import com.spartan.bolao.service.MatchLockedException
import com.spartan.bolao.service.MatchNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ApiError(val message: String)

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(MatchNotFoundException::class)
    fun handleNotFound(ex: MatchNotFoundException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError(ex.message ?: "Não encontrado"))

    @ExceptionHandler(MatchLockedException::class)
    fun handleLocked(ex: MatchLockedException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError(ex.message ?: "Palpite bloqueado"))

    @ExceptionHandler(EmailAlreadyUsedException::class)
    fun handleEmailUsed(ex: EmailAlreadyUsedException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError(ex.message ?: "E-mail em uso"))

    @ExceptionHandler(InvalidCredentialsException::class, UnauthenticatedException::class, GoogleAuthException::class)
    fun handleUnauthorized(ex: RuntimeException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError(ex.message ?: "Não autorizado"))

    @ExceptionHandler(GoogleNotConfiguredException::class)
    fun handleGoogleOff(ex: GoogleNotConfiguredException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiError(ex.message ?: "Indisponível"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val msg = ex.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
            .ifBlank { "Dados inválidos" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError(msg))
    }
}
