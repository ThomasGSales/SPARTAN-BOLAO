package com.spartan.bolao.service

import com.spartan.bolao.domain.Match
import com.spartan.bolao.repository.GuessRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Regras de pontuação:
 *  - 5 pontos: placar exato
 *  - 3 pontos: acertou o vencedor (ou empate) E o saldo de gols
 *  - 1 ponto:  acertou só o vencedor (ou que foi empate)
 *  - 0 ponto:  errou o resultado
 */
@Service
class ScoringService(
    private val guessRepository: GuessRepository
) {

    fun calculate(
        actualHome: Int,
        actualAway: Int,
        guessHome: Int,
        guessAway: Int
    ): Int {
        // Placar exato
        if (actualHome == guessHome && actualAway == guessAway) return 5

        val actualOutcome = Integer.signum(actualHome - actualAway) // -1, 0, +1
        val guessOutcome = Integer.signum(guessHome - guessAway)
        if (actualOutcome != guessOutcome) return 0 // errou quem venceu/empate

        // Acertou o resultado; confere o saldo de gols
        val actualDiff = actualHome - actualAway
        val guessDiff = guessHome - guessAway
        return if (actualDiff == guessDiff) 3 else 1
    }

    /** Recalcula e grava os pontos de todos os palpites de um jogo encerrado. */
    @Transactional
    fun applyForMatch(match: Match) {
        val home = match.homeScore ?: return
        val away = match.awayScore ?: return
        val guesses = guessRepository.findByMatchId(match.id!!)
        guesses.forEach { g ->
            g.pointsEarned = calculate(home, away, g.homeScoreGuess, g.awayScoreGuess)
        }
        guessRepository.saveAll(guesses)
    }
}
