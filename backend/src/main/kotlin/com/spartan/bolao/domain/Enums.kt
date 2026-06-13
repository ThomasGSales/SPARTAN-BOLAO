package com.spartan.bolao.domain

enum class UserRole { USER, ADMIN }

// Copa 2026 (48 seleções) tem 16-avos (ROUND_OF_32) antes das oitavas.
enum class MatchPhase { GROUP, ROUND_OF_32, ROUND_OF_16, QUARTER, SEMI, THIRD_PLACE, FINAL }

enum class MatchStatus { SCHEDULED, LIVE, FINISHED, CANCELLED }
