package com.spartan.bolao.football

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.OffsetDateTime

/**
 * Cliente da football-data.org (v4) — fonte oficial dos jogos da Copa.
 * Plano grátis: competição "WC", 10 req/min, placares finais (sem live em tempo real,
 * o que basta para o bolão, que pontua no encerramento).
 */
@Component
class FootballDataClient(
    @Value("\${app.football.base-url:https://api.football-data.org/v4}") private val baseUrl: String,
    @Value("\${app.football.token:}") private val token: String,
    @Value("\${app.football.competition:WC}") private val competition: String,
) {
    private val rest: RestClient = RestClient.create()

    /** Só sincroniza se houver token configurado. */
    val isConfigured: Boolean get() = token.isNotBlank()

    /** Todos os jogos da competição (104 na Copa 2026). */
    fun fetchMatches(): List<FdMatch> {
        val resp = rest.get()
            .uri("$baseUrl/competitions/$competition/matches")
            .header("X-Auth-Token", token)
            .retrieve()
            .body(FdMatchesResponse::class.java)
        return resp?.matches ?: emptyList()
    }
}

// ----- DTOs (só os campos que usamos; ignora o resto) -----

@JsonIgnoreProperties(ignoreUnknown = true)
data class FdMatchesResponse(val matches: List<FdMatch> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
data class FdMatch(
    val id: Long,
    val utcDate: OffsetDateTime,
    val status: String,
    val stage: String,
    val group: String?,
    val homeTeam: FdTeam,
    val awayTeam: FdTeam,
    val score: FdScore = FdScore(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FdTeam(
    val id: Long? = null,
    val name: String? = null,
    val tla: String? = null,
    val crest: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FdScore(val fullTime: FdScoreTime = FdScoreTime())

@JsonIgnoreProperties(ignoreUnknown = true)
data class FdScoreTime(val home: Int? = null, val away: Int? = null)
