package com.tnear.adoptloop.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(name = "admins")
@EntityListeners(AuditingEntityListener::class)
class Admin(
    @Column(nullable = false) var name: String,
    @Column(name = "key_hash", nullable = false, unique = true) var keyHash: String,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.EPOCH
}
