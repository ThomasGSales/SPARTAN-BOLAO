package com.spartan.bolao.service

import com.spartan.bolao.repository.UserRepository
import com.spartan.bolao.security.CurrentUserProvider
import com.spartan.bolao.web.dto.RankingEntryDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RankingService(
    private val userRepository: UserRepository,
    private val currentUserProvider: CurrentUserProvider
) {

    @Transactional(readOnly = true)
    fun leaderboard(): List<RankingEntryDto> {
        val meId = currentUserProvider.get().id
        return userRepository.ranking().map { row ->
            RankingEntryDto(
                position = row.position,
                userId = row.userId,
                name = row.name,
                avatarUrl = row.avatarUrl,
                totalPoints = row.totalPoints,
                exactHits = row.exactHits,
                totalGuesses = row.totalGuesses,
                isMe = row.userId == meId
            )
        }
    }
}
