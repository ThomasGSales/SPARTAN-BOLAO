package com.spartan.bolao.web.dto

import java.util.UUID

data class RankingEntryDto(
    val position: Long,
    val userId: UUID,
    val name: String,
    val avatarUrl: String?,
    val totalPoints: Long,
    val exactHits: Long,
    val totalGuesses: Long,
    val isMe: Boolean
)
