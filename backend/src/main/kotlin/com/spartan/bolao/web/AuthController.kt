package com.spartan.bolao.web

import com.spartan.bolao.security.GoogleTokenVerifier
import com.spartan.bolao.service.AuthService
import com.spartan.bolao.web.dto.AuthConfigResponse
import com.spartan.bolao.web.dto.AuthResponse
import com.spartan.bolao.web.dto.GoogleLoginRequest
import com.spartan.bolao.web.dto.LoginRequest
import com.spartan.bolao.web.dto.RegisterRequest
import com.spartan.bolao.web.dto.UserDto
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// context-path "/api" + "/auth"
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val googleTokenVerifier: GoogleTokenVerifier,
    @Value("\${app.google.client-id:}") private val googleClientId: String
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody body: RegisterRequest): AuthResponse =
        authService.register(body)

    @PostMapping("/login")
    fun login(@Valid @RequestBody body: LoginRequest): AuthResponse =
        authService.login(body)

    @PostMapping("/google")
    fun google(@Valid @RequestBody body: GoogleLoginRequest): AuthResponse =
        authService.loginWithGoogle(body)

    @GetMapping("/me")
    fun me(): UserDto = authService.me()

    /** Config pública: o front usa para habilitar (ou não) o botão Google. */
    @GetMapping("/config")
    fun config(): AuthConfigResponse =
        AuthConfigResponse(
            googleEnabled = googleTokenVerifier.isConfigured,
            googleClientId = googleClientId.ifBlank { null }
        )
}
