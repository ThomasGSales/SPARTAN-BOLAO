package com.spartan.bolao.config

import com.nimbusds.jose.jwk.source.ImmutableSecret
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.core.convert.converter.Converter
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.security.MessageDigest
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration
class SecurityConfig(
    @Value("\${app.jwt.secret}") private val jwtSecret: String
) {

    /**
     * Deriva uma chave HMAC de 256 bits a partir do segredo configurado
     * (via SHA-256), garantindo tamanho válido para HS256 independente
     * do comprimento do segredo no .env.
     */
    private val secretKey: SecretKey by lazy {
        val keyBytes = MessageDigest.getInstance("SHA-256").digest(jwtSecret.toByteArray())
        SecretKeySpec(keyBytes, "HmacSHA256")
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            cors { }
            // Paths são relativos ao context-path (/api), então use "/auth/**".
            authorizeHttpRequests {
                authorize(HttpMethod.OPTIONS, "/**", permitAll)
                authorize("/auth/**", permitAll)
                authorize("/admin/**", hasRole("ADMIN"))
                authorize(anyRequest, authenticated)
            }
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }
            oauth2ResourceServer {
                jwt {
                    jwtAuthenticationConverter = roleAwareJwtConverter()
                }
            }
            httpBasic { disable() }
            formLogin { disable() }
        }
        return http.build()
    }

    /** Mapeia o claim `role` do JWT para a authority ROLE_<role>. */
    private fun roleAwareJwtConverter(): JwtAuthenticationConverter {
        val authoritiesConverter = Converter<Jwt, Collection<GrantedAuthority>> { jwt ->
            val role = jwt.getClaimAsString("role") ?: "USER"
            listOf(SimpleGrantedAuthority("ROLE_$role"))
        }
        return JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter(authoritiesConverter)
        }
    }

    @Bean
    fun jwtEncoder(): JwtEncoder = NimbusJwtEncoder(ImmutableSecret(secretKey))

    @Bean
    fun jwtDecoder(): JwtDecoder =
        NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build()

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = listOf("http://localhost:4200")
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}
