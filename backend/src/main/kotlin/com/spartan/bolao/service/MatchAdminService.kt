package com.spartan.bolao.service

import com.spartan.bolao.domain.MatchStatus
import com.spartan.bolao.repository.MatchRepository
import com.spartan.bolao.web.dto.MatchDto
import com.spartan.bolao.web.dto.toDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MatchAdminService(
    private val matchRepository: MatchRepository,
    private val scoringService: ScoringService
) {

    /** Lança o placar oficial, encerra o jogo e pontua todos os palpites. */
    @Transactional
    fun setResult(matchId: UUID, home: Int, away: Int): MatchDto {
        val match = matchRepository.findById(matchId)
            .orElseThrow { MatchNotFoundException(matchId) }
        match.homeScore = home
        match.awayScore = away
        match.status = MatchStatus.FINISHED
        matchRepository.save(match)
        scoringService.applyForMatch(match)
        return match.toDto()
    }
}
