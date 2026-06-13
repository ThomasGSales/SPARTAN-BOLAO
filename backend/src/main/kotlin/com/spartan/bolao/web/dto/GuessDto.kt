package com.spartan.bolao.web.dto

import com.spartan.bolao.domain.Guess
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.OffsetDateTime
import java.util.UUID

/** Upsert de um único palpite (corpo do PUT /guesses/{matchId}). */
data class GuessRequest(
    @field:Min(0) @field:Max(99) val homeScoreGuess: Int,
    @field:Min(0) @field:Max(99) val awayScoreGuess: Int
)

/** Item do "Salvar Tudo". */
data class BulkGuessItem(
    val matchId: UUID,
    @field:Min(0) @field:Max(99) val homeScoreGuess: Int,
    @field:Min(0) @field:Max(99) val awayScoreGuess: Int
)

data class BulkGuessRequest(
    @field:Valid val guesses: List<BulkGuessItem>
)

/** Palpite devolvido pela API. */
data class GuessResponse(
    val id: UUID,
    val matchId: UUID,
    val homeScoreGuess: Int,
    val awayScoreGuess: Int,
    val pointsEarned: Int,
    val updatedAt: OffsetDateTime
)

fun Guess.toResponse(): GuessResponse = GuessResponse(
    id = id!!,
    matchId = match.id!!,
    homeScoreGuess = homeScoreGuess,
    awayScoreGuess = awayScoreGuess,
    pointsEarned = pointsEarned,
    updatedAt = updatedAt
)
