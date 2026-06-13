package com.spartan.bolao.web

import com.spartan.bolao.service.GuessService
import com.spartan.bolao.web.dto.BulkGuessRequest
import com.spartan.bolao.web.dto.GuessRequest
import com.spartan.bolao.web.dto.GuessResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

// context-path "/api" + "/guesses"
@RestController
@RequestMapping("/guesses")
class GuessController(
    private val guessService: GuessService
) {

    /** Palpites do usuário atual. */
    @GetMapping
    fun mine(): List<GuessResponse> = guessService.myGuesses()

    /** Cria/atualiza um palpite (auto-save). */
    @PutMapping("/{matchId}")
    fun upsert(
        @PathVariable matchId: UUID,
        @Valid @RequestBody body: GuessRequest
    ): GuessResponse = guessService.upsert(matchId, body)

    /** Salva vários palpites de uma vez ("Salvar Tudo"). */
    @PostMapping("/bulk")
    fun bulk(@Valid @RequestBody body: BulkGuessRequest): List<GuessResponse> =
        guessService.upsertBulk(body)
}
