package com.spartan.bolao.security

import com.spartan.bolao.domain.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class JwtService(
    private val jwtEncoder: JwtEncoder,
    @Value("\${app.jwt.expiration-minutes:720}") private val expirationMinutes: Long
) {

    /** Gera um JWT assinado (HS256) com os dados do usuário. */
    fun generate(user: User): String {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .issuer("spartan")
            .issuedAt(now)
            .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
            .subject(user.id.toString())
            .claim("email", user.email)
            .claim("name", user.name)
            .claim("role", user.role.name)
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
    }
}
