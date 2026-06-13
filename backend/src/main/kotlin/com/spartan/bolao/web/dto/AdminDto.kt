package com.spartan.bolao.web.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class SetResultRequest(
    @field:Min(0) @field:Max(99) val homeScore: Int,
    @field:Min(0) @field:Max(99) val awayScore: Int
)
