package com.spartan.bolao.repository

import com.spartan.bolao.domain.Guess
import com.spartan.bolao.domain.Match
import com.spartan.bolao.domain.MatchPhase
import com.spartan.bolao.domain.Team
import com.spartan.bolao.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun findByGoogleId(googleId: String): User?

    /** Lê a VIEW v_ranking (aliases entre aspas para casar com a projeção camelCase). */
    @Query(
        value = """
        SELECT user_id      AS "userId",
               name         AS "name",
               avatar_url   AS "avatarUrl",
               total_points AS "totalPoints",
               exact_hits   AS "exactHits",
               total_hits   AS "totalHits",
               total_guesses AS "totalGuesses",
               position     AS "position"
        FROM v_ranking
        """,
        nativeQuery = true
    )
    fun ranking(): List<RankingRow>
}

/** Projeção (read-only) de uma linha do ranking. */
interface RankingRow {
    val userId: UUID
    val name: String
    val avatarUrl: String?
    val totalPoints: Long
    val exactHits: Long
    val totalHits: Long
    val totalGuesses: Long
    val position: Long
}

interface TeamRepository : JpaRepository<Team, UUID> {
    fun findByCode(code: String): Team?
    fun findByExternalId(externalId: Long): Team?
}

interface MatchRepository : JpaRepository<Match, UUID> {

    // Carrega os times junto (evita N+1) e ordena cronologicamente.
    @Query(
        """
        SELECT m FROM Match m
        JOIN FETCH m.homeTeam
        JOIN FETCH m.awayTeam
        ORDER BY m.matchDatetime ASC
        """
    )
    fun findAllOrdered(): List<Match>

    @Query(
        """
        SELECT m FROM Match m
        JOIN FETCH m.homeTeam
        JOIN FETCH m.awayTeam
        WHERE m.phase = :phase
        ORDER BY m.matchDatetime ASC
        """
    )
    fun findByPhaseOrdered(phase: MatchPhase): List<Match>

    fun findByExternalId(externalId: Long): Match?
}

interface GuessRepository : JpaRepository<Guess, UUID> {
    fun findByUserId(userId: UUID): List<Guess>
    fun findByUserIdAndMatchId(userId: UUID, matchId: UUID): Guess?
    fun findByMatchId(matchId: UUID): List<Guess>
}
