package com.spartan.bolao.web

import com.spartan.bolao.football.WorldCupSyncService
import com.spartan.bolao.service.MatchAdminService
import com.spartan.bolao.web.dto.MatchDto
import com.spartan.bolao.web.dto.SetResultRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

// context-path "/api" + "/admin" — protegido por ROLE_ADMIN no SecurityConfig.
@RestController
@RequestMapping("/admin")
class AdminController(
    private val matchAdminService: MatchAdminService,
    private val worldCupSyncService: WorldCupSyncService,
) {

    @PatchMapping("/matches/{id}/result")
    fun setResult(
        @PathVariable id: UUID,
        @Valid @RequestBody body: SetResultRequest
    ): MatchDto = matchAdminService.setResult(id, body.homeScore, body.awayScore)

    /** Força uma sincronização imediata com a football-data.org. */
    @PostMapping("/sync")
    fun sync(): WorldCupSyncService.SyncResult = worldCupSyncService.sync()
}
