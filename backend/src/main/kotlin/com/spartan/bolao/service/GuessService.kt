package com.spartan.bolao.service

import com.spartan.bolao.domain.Guess
import com.spartan.bolao.domain.Match
import com.spartan.bolao.domain.User
import com.spartan.bolao.repository.GuessRepository
import com.spartan.bolao.repository.MatchRepository
import com.spartan.bolao.security.CurrentUserProvider
import com.spartan.bolao.web.dto.BulkGuessRequest
import com.spartan.bolao.web.dto.GuessRequest
import com.spartan.bolao.web.dto.GuessResponse
import com.spartan.bolao.web.dto.toResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

class MatchNotFoundException(id: UUID) : RuntimeException("Jogo não encontrado: $id")
class MatchLockedException : RuntimeException("Este jogo já começou; os palpites estão bloqueados.")

@Service
class GuessService(
    private val guessRepository: GuessRepository,
    private val matchRepository: MatchRepository,
    private val currentUserProvider: CurrentUserProvider
) {

    @Transactional(readOnly = true)
    fun myGuesses(): List<GuessResponse> {
        val user = currentUserProvider.get()
        return guessRepository.findByUserId(user.id!!).map { it.toResponse() }
    }

    /** Cria ou atualiza o palpite do usuário para um jogo. Idempotente. */
    @Transactional
    fun upsert(matchId: UUID, req: GuessRequest): GuessResponse {
        val user = currentUserProvider.get()
        val match = matchRepository.findById(matchId)
            .orElseThrow { MatchNotFoundException(matchId) }
        if (match.isLocked) throw MatchLockedException()
        return persist(user, match, req.homeScoreGuess, req.awayScoreGuess).toResponse()
    }

    /**
     * "Salvar Tudo": grava todos os palpites válidos.
     * Jogos inexistentes ou já bloqueados são ignorados silenciosamente
     * (o front não deveria enviá-los, mas garantimos robustez aqui).
     */
    @Transactional
    fun upsertBulk(req: BulkGuessRequest): List<GuessResponse> {
        val user = currentUserProvider.get()
        return req.guesses.mapNotNull { item ->
            val match = matchRepository.findById(item.matchId).orElse(null) ?: return@mapNotNull null
            if (match.isLocked) return@mapNotNull null
            persist(user, match, item.homeScoreGuess, item.awayScoreGuess).toResponse()
        }
    }

    private fun persist(user: User, match: Match, home: Int, away: Int): Guess {
        val existing = guessRepository.findByUserIdAndMatchId(user.id!!, match.id!!)
        val guess = if (existing != null) {
            existing.homeScoreGuess = home
            existing.awayScoreGuess = away
            existing
        } else {
            Guess(user = user, match = match, homeScoreGuess = home, awayScoreGuess = away)
        }
        return guessRepository.save(guess)
    }
}
