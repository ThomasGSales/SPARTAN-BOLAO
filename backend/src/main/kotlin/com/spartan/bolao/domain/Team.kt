package com.spartan.bolao.domain

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "teams")
class Team(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, unique = true, length = 80)
    var name: String,

    /** Código FIFA de 3 letras: "BRA", "ARG", "SRB". */
    @Column(nullable = false, unique = true, length = 3)
    var code: String,

    @Column(name = "flag_url")
    var flagUrl: String? = null,

    /** Id da seleção na football-data.org (chave de upsert da sincronização). */
    @Column(name = "external_id", unique = true)
    var externalId: Long? = null
)
