package com.spartan.bolao.web.dto

import com.spartan.bolao.domain.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class RegisterRequest(
    @field:NotBlank @field:Size(min = 2, max = 120) val name: String,
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank @field:Size(min = 6, max = 100) val password: String
)

data class LoginRequest(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val password: String
)

data class GoogleLoginRequest(
    @field:NotBlank val idToken: String
)

/** Usuário exposto ao front (nunca inclui hash de senha). */
data class UserDto(
    val id: UUID,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val role: String
)

data class AuthResponse(
    val token: String,
    val user: UserDto
)

/** Config pública consumida pela tela de login (habilita ou não o botão Google). */
data class AuthConfigResponse(
    val googleEnabled: Boolean,
    val googleClientId: String?
)

fun User.toDto(): UserDto = UserDto(
    id = id!!,
    name = name,
    email = email,
    avatarUrl = avatarUrl,
    role = role.name
)
