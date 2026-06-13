package com.spartan.bolao.web

import com.spartan.bolao.service.RankingService
import com.spartan.bolao.web.dto.RankingEntryDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// context-path "/api" + "/ranking"
@RestController
@RequestMapping("/ranking")
class RankingController(
    private val rankingService: RankingService
) {

    @GetMapping
    fun leaderboard(): List<RankingEntryDto> = rankingService.leaderboard()
}
