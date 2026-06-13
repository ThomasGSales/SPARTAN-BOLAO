package com.spartan.bolao.service

import com.spartan.bolao.domain.MatchPhase
import com.spartan.bolao.repository.MatchRepository
import com.spartan.bolao.web.dto.MatchDto
import com.spartan.bolao.web.dto.toDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MatchService(
    private val matchRepository: MatchRepository
) {

    /** Lista todos os jogos, ou apenas os de uma fase se [phase] vier preenchido. */
    @Transactional(readOnly = true)
    fun list(phase: MatchPhase?): List<MatchDto> {
        val matches = if (phase == null) {
            matchRepository.findAllOrdered()
        } else {
            matchRepository.findByPhaseOrdered(phase)
        }
        return matches.map { it.toDto() }
    }
}
