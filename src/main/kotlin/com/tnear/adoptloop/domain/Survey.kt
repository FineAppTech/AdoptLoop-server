package com.tnear.adoptloop.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(name = "surveys")
@EntityListeners(AuditingEntityListener::class)
class Survey(
    @Column(name = "adoption_id", nullable = false) var adoptionId: Long,
    @Column(nullable = false) var title: String,
    @Column(name = "public_slug", nullable = false, unique = true) var publicSlug: String,
    @Column(nullable = false) var deadline: Instant,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: SurveyStatus = SurveyStatus.DRAFT,
    @Column(name = "published_at") var publishedAt: Instant? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) var createdAt: Instant = Instant.EPOCH
    @LastModifiedDate @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.EPOCH
}
