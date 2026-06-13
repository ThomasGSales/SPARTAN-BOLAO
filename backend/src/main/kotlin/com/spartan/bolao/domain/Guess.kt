package com.spartan.bolao.domain

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "guesses",
    uniqueConstraints = [UniqueConstraint(name = "uq_user_match", columnNames = ["user_id", "match_id"])]
)
class Guess(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id")
    var match: Match,

    @Column(name = "home_score_guess", nullable = false)
    var homeScoreGuess: Int,

    @Column(name = "away_score_guess", nullable = false)
    var awayScoreGuess: Int,

    /** Preenchido pelo motor de pontuação quando o jogo termina. */
    @Column(name = "points_earned", nullable = false)
    var pointsEarned: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
