package com.spartan.bolao.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

class GoogleNotConfiguredException : RuntimeException("Login com Google não está configurado neste servidor.")
class GoogleAuthException(msg: String) : RuntimeException(msg)

/** Dados extraídos do ID token do Google. */
data class GoogleProfile(
    val googleId: String,
    val email: String,
    val name: String,
    val pictureUrl: String?
)

/**
 * Verifica o ID token do Google chamando o endpoint oficial `tokeninfo`.
 * Confere a assinatura/validade (o Google faz isso) e a audiência (nosso client-id).
 */
@Component
class GoogleTokenVerifier(
    @Value("\${app.google.client-id:}") private val clientId: String
) {
    private val restClient = RestClient.create()

    val isConfigured: Boolean get() = clientId.isNotBlank()

    @Suppress("UNCHECKED_CAST")
    fun verify(idToken: String): GoogleProfile {
        if (!isConfigured) throw GoogleNotConfiguredException()

        val info = try {
            restClient.get()
                .uri("https://oauth2.googleapis.com/tokeninfo?id_token={t}", idToken)
                .retrieve()
                .body(Map::class.java) as? Map<String, Any?>
        } catch (e: Exception) {
            throw GoogleAuthException("Token do Google inválido.")
        } ?: throw GoogleAuthException("Token do Google inválido.")

        val aud = info["aud"] as? String
        if (aud != clientId) throw GoogleAuthException("Token do Google não pertence a esta aplicação.")

        val emailVerified = info["email_verified"].toString() == "true"
        if (!emailVerified) throw GoogleAuthException("E-mail do Google não verificado.")

        val sub = info["sub"] as? String ?: throw GoogleAuthException("Token do Google sem identificador.")
        val email = info["email"] as? String ?: throw GoogleAuthException("Token do Google sem e-mail.")

        return GoogleProfile(
            googleId = sub,
            email = email,
            name = (info["name"] as? String) ?: email.substringBefore("@"),
            pictureUrl = info["picture"] as? String
        )
    }
}
