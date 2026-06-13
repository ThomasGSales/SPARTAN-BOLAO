package com.spartan.bolao.domain

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "matches")
class Match(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "home_team_id")
    var homeTeam: Team,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "away_team_id")
    var awayTeam: Team,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "match_phase")
    var phase: MatchPhase,

    /** "A".."H" na fase de grupos; NULL no mata-mata. */
    @Column(name = "group_label", length = 2)
    var groupLabel: String? = null,

    @Column(name = "match_datetime", nullable = false)
    var matchDatetime: OffsetDateTime,

    @Column(name = "home_score")
    var homeScore: Int? = null,

    @Column(name = "away_score")
    var awayScore: Int? = null,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "match_status")
    var status: MatchStatus = MatchStatus.SCHEDULED,

    /** Id do jogo na football-data.org (chave de upsert da sincronização). */
    @Column(name = "external_id", unique = true)
    var externalId: Long? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()
) {
    /** Regra de negócio: palpites travam quando o jogo começa. */
    val isLocked: Boolean
        get() = status != MatchStatus.SCHEDULED || OffsetDateTime.now().isAfter(matchDatetime)
}
