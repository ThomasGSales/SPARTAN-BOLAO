package com.spartan.bolao.domain

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "users")
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, length = 120)
    var name: String,

    @Column(nullable = false, unique = true, length = 180)
    var email: String,

    /** NULL para usuários que entram só via Google. */
    @Column(name = "password_hash")
    var passwordHash: String? = null,

    /** NULL para usuários tradicionais (email/senha). */
    @Column(name = "google_id", unique = true, length = 120)
    var googleId: String? = null,

    @Column(name = "avatar_url")
    var avatarUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "user_role")
    var role: UserRole = UserRole.USER,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
