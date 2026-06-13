package com.spartan.bolao.service

import com.spartan.bolao.domain.User
import com.spartan.bolao.domain.UserRole
import com.spartan.bolao.repository.UserRepository
import com.spartan.bolao.security.CurrentUserProvider
import com.spartan.bolao.security.GoogleTokenVerifier
import com.spartan.bolao.security.JwtService
import com.spartan.bolao.web.dto.AuthResponse
import com.spartan.bolao.web.dto.GoogleLoginRequest
import com.spartan.bolao.web.dto.LoginRequest
import com.spartan.bolao.web.dto.RegisterRequest
import com.spartan.bolao.web.dto.UserDto
import com.spartan.bolao.web.dto.toDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

class EmailAlreadyUsedException : RuntimeException("Este e-mail já está cadastrado.")
class InvalidCredentialsException : RuntimeException("E-mail ou senha inválidos.")

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val googleTokenVerifier: GoogleTokenVerifier,
    private val currentUserProvider: CurrentUserProvider,
    @Value("\${app.admin-emails:}") adminEmailsRaw: String
) {
    // E-mails que devem ter papel ADMIN (definidos via ADMIN_EMAILS no .env).
    private val adminEmails: Set<String> =
        adminEmailsRaw.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()

    private fun roleFor(email: String): UserRole =
        if (email in adminEmails) UserRole.ADMIN else UserRole.USER

    @Transactional
    fun register(req: RegisterRequest): AuthResponse {
        val email = req.email.trim().lowercase()
        if (userRepository.findByEmail(email) != null) throw EmailAlreadyUsedException()
        val user = userRepository.save(
            User(
                name = req.name.trim(),
                email = email,
                passwordHash = passwordEncoder.encode(req.password),
                role = roleFor(email)
            )
        )
        return AuthResponse(jwtService.generate(user), user.toDto())
    }

    @Transactional
    fun login(req: LoginRequest): AuthResponse {
        val email = req.email.trim().lowercase()
        val user = userRepository.findByEmail(email) ?: throw InvalidCredentialsException()
        val hash = user.passwordHash ?: throw InvalidCredentialsException() // conta só-Google
        if (!passwordEncoder.matches(req.password, hash)) throw InvalidCredentialsException()
        promoteIfNeeded(user)
        return AuthResponse(jwtService.generate(user), user.toDto())
    }

    /** Login/cadastro via Google. Vincula ao usuário existente pelo e-mail se já houver. */
    @Transactional
    fun loginWithGoogle(req: GoogleLoginRequest): AuthResponse {
        val profile = googleTokenVerifier.verify(req.idToken)
        val email = profile.email.trim().lowercase()

        val user = userRepository.findByGoogleId(profile.googleId)
            ?: userRepository.findByEmail(email)?.also { existing ->
                // já tinha conta por e-mail: vincula o Google
                existing.googleId = profile.googleId
                if (existing.avatarUrl == null) existing.avatarUrl = profile.pictureUrl
            }
            ?: User(
                name = profile.name,
                email = email,
                googleId = profile.googleId,
                avatarUrl = profile.pictureUrl,
                role = roleFor(email)
            )

        promoteIfNeeded(user)
        val saved = userRepository.save(user)
        return AuthResponse(jwtService.generate(saved), saved.toDto())
    }

    /** Mantém o papel em dia com a lista de admins (promove no login). */
    private fun promoteIfNeeded(user: User) {
        val expected = roleFor(user.email)
        if (expected == UserRole.ADMIN && user.role != UserRole.ADMIN) {
            user.role = UserRole.ADMIN
        }
    }

    @Transactional(readOnly = true)
    fun me(): UserDto = currentUserProvider.get().toDto()
}
