package com.spartan.bolao.web.dto

import com.spartan.bolao.domain.Match
import com.spartan.bolao.domain.MatchPhase
import com.spartan.bolao.domain.MatchStatus
import java.time.OffsetDateTime
import java.util.UUID

/** Time enxuto para a UI (só o que a tela de palpites precisa). */
data class TeamDto(
    val id: UUID,
    val name: String,
    val code: String,
    val flagUrl: String?
)

/** Representação de um jogo exposta pela API. */
data class MatchDto(
    val id: UUID,
    val phase: MatchPhase,
    val groupLabel: String?,
    val matchDatetime: OffsetDateTime,
    val status: MatchStatus,
    val locked: Boolean,
    val homeTeam: TeamDto,
    val awayTeam: TeamDto,
    val homeScore: Int?,
    val awayScore: Int?
)

fun Match.toDto(): MatchDto = MatchDto(
    id = id!!,
    phase = phase,
    groupLabel = groupLabel,
    matchDatetime = matchDatetime,
    status = status,
    locked = isLocked,
    homeTeam = homeTeam.toTeamDto(),
    awayTeam = awayTeam.toTeamDto(),
    homeScore = homeScore,
    awayScore = awayScore
)

private fun com.spartan.bolao.domain.Team.toTeamDto() =
    TeamDto(id = id!!, name = name, code = code, flagUrl = flagUrl)
