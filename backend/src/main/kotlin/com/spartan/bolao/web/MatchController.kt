package com.spartan.bolao.web

import com.spartan.bolao.domain.MatchPhase
import com.spartan.bolao.service.MatchService
import com.spartan.bolao.web.dto.MatchDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// context-path "/api" (application.yml) + "/matches" => GET /api/matches
@RestController
@RequestMapping("/matches")
class MatchController(
    private val matchService: MatchService
) {

    /**
     * Lista os jogos.
     *   GET /api/matches            -> todos
     *   GET /api/matches?phase=GROUP -> só a fase de grupos
     */
    @GetMapping
    fun list(@RequestParam(required = false) phase: MatchPhase?): List<MatchDto> =
        matchService.list(phase)
}
