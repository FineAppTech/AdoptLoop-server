package com.tnear.adoptloop.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(name = "adoptions")
@EntityListeners(AuditingEntityListener::class)
class Adoption(
    @Column(name = "admin_id", nullable = false) var adminId: Long,
    @Column(nullable = false) var name: String,
    @Column(nullable = false, columnDefinition = "TEXT") var goal: String,
    @Column(name = "target_audience", nullable = false, columnDefinition = "TEXT") var targetAudience: String,
    @Column(columnDefinition = "TEXT") var concern: String? = null,
    @Column(name = "target_count", nullable = false) var targetCount: Int,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: AdoptionStatus = AdoptionStatus.ACTIVE,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) var createdAt: Instant = Instant.EPOCH
    @LastModifiedDate @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.EPOCH
}
