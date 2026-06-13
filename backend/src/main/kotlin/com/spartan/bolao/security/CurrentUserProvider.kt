package com.spartan.bolao.security

import com.spartan.bolao.domain.User
import com.spartan.bolao.repository.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

class UnauthenticatedException : RuntimeException("Não autenticado.")

/**
 * Resolve o usuário autenticado a partir do JWT (claim `sub` = id do usuário).
 */
@Component
class CurrentUserProvider(
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    fun get(): User {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw UnauthenticatedException()
        val userId = runCatching { UUID.fromString(auth.name) }
            .getOrElse { throw UnauthenticatedException() }
        return userRepository.findById(userId).orElseThrow { UnauthenticatedException() }
    }
}
