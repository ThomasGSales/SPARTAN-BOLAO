package com.spartan.bolao.football

import com.spartan.bolao.domain.Match
import com.spartan.bolao.domain.MatchPhase
import com.spartan.bolao.domain.MatchStatus
import com.spartan.bolao.domain.Team
import com.spartan.bolao.repository.MatchRepository
import com.spartan.bolao.repository.TeamRepository
import com.spartan.bolao.service.ScoringService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Sincroniza times, jogos e placares da Copa 2026 a partir da football-data.org.
 * É a fonte da verdade: faz *upsert* idempotente por `external_id` e, quando um
 * jogo encerra, dispara a pontuação dos palpites (reaproveita o ScoringService).
 *
 * Regras importantes:
 *  - Só cria/atualiza jogos com AMBOS os times definidos (mata-mata só entra
 *    quando os classificados são conhecidos).
 *  - Nunca "rebaixa" um jogo já FINISHED localmente (o admin pode ter lançado
 *    o resultado antes da API).
 */
@Service
class WorldCupSyncService(
    private val client: FootballDataClient,
    private val teamRepository: TeamRepository,
    private val matchRepository: MatchRepository,
    private val scoringService: ScoringService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class SyncResult(val fetched: Int, val upserted: Int, val scored: Int, val skipped: Int)

    @Transactional
    fun sync(): SyncResult {
        if (!client.isConfigured) {
            log.debug("Sync ignorada: FOOTBALL_DATA_TOKEN não configurado.")
            return SyncResult(0, 0, 0, 0)
        }

        val matches = try {
            client.fetchMatches()
        } catch (e: Exception) {
            log.warn("Falha ao buscar jogos da football-data.org: {}", e.message)
            return SyncResult(0, 0, 0, 0)
        }

        var upserted = 0
        var scored = 0
        var skipped = 0

        for (fm in matches) {
            val phase = mapStage(fm.stage)
            if (phase == null) {
                skipped++; continue
            }
            // Mata-mata indefinido (times null) entra depois.
            val home = upsertTeam(fm.homeTeam)
            val away = upsertTeam(fm.awayTeam)
            if (home == null || away == null || home === away) {
                skipped++; continue
            }

            val existing = matchRepository.findByExternalId(fm.id)
            val oldHome = existing?.homeScore
            val oldAway = existing?.awayScore
            val wasFinished = existing?.status == MatchStatus.FINISHED

            val match = existing ?: Match(
                homeTeam = home,
                awayTeam = away,
                phase = phase,
                matchDatetime = fm.utcDate,
                externalId = fm.id,
            )
            match.homeTeam = home
            match.awayTeam = away
            match.phase = phase
            match.groupLabel = fm.group?.removePrefix("GROUP_")
            match.matchDatetime = fm.utcDate

            val apiStatus = mapStatus(fm.status)
            val newHome = fm.score.fullTime.home
            val newAway = fm.score.fullTime.away

            if (apiStatus == MatchStatus.FINISHED) {
                match.status = MatchStatus.FINISHED
                match.homeScore = newHome
                match.awayScore = newAway
            } else if (!wasFinished) {
                // Não rebaixa um jogo já encerrado localmente.
                match.status = apiStatus
                match.homeScore = newHome
                match.awayScore = newAway
            }

            val saved = matchRepository.save(match)
            upserted++

            // Pontua só quando faz sentido: encerrou agora ou o placar mudou.
            val finishedWithScore = saved.status == MatchStatus.FINISHED &&
                saved.homeScore != null && saved.awayScore != null
            val scoreChanged = saved.homeScore != oldHome || saved.awayScore != oldAway
            if (finishedWithScore && (!wasFinished || scoreChanged)) {
                scoringService.applyForMatch(saved)
                scored++
            }
        }

        if (upserted > 0 || scored > 0) {
            log.info("Sync Copa: {} jogos da API, {} upserts, {} pontuados, {} ignorados.",
                matches.size, upserted, scored, skipped)
        }
        return SyncResult(matches.size, upserted, scored, skipped)
    }

    /** Acha (por external_id) ou cria a seleção; atualiza nome/escudo se mudou. */
    private fun upsertTeam(t: FdTeam): Team? {
        val ext = t.id ?: return null
        val code = t.tla?.take(3) ?: return null
        val existing = teamRepository.findByExternalId(ext)
        if (existing != null) {
            t.name?.let { existing.name = it }
            t.crest?.let { existing.flagUrl = it }
            return existing
        }
        return teamRepository.save(
            Team(name = t.name ?: code, code = code, flagUrl = t.crest, externalId = ext)
        )
    }

    private fun mapStage(stage: String): MatchPhase? = when (stage) {
        "GROUP_STAGE" -> MatchPhase.GROUP
        "LAST_32" -> MatchPhase.ROUND_OF_32
        "LAST_16" -> MatchPhase.ROUND_OF_16
        "QUARTER_FINALS" -> MatchPhase.QUARTER
        "SEMI_FINALS" -> MatchPhase.SEMI
        "THIRD_PLACE" -> MatchPhase.THIRD_PLACE
        "FINAL" -> MatchPhase.FINAL
        else -> null
    }

    private fun mapStatus(status: String): MatchStatus = when (status) {
        "IN_PLAY", "PAUSED" -> MatchStatus.LIVE
        "FINISHED", "AWARDED" -> MatchStatus.FINISHED
        "CANCELLED" -> MatchStatus.CANCELLED
        else -> MatchStatus.SCHEDULED // SCHEDULED, TIMED, POSTPONED, SUSPENDED
    }
}
