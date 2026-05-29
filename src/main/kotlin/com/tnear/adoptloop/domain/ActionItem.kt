package com.tnear.adoptloop.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(name = "action_items")
@EntityListeners(AuditingEntityListener::class)
class ActionItem(
    @Column(name = "adoption_id", nullable = false) var adoptionId: Long,
    @Column(name = "analysis_id", nullable = false) var analysisId: Long,
    @Column(nullable = false) var title: String,
    @Column(columnDefinition = "TEXT") var description: String? = null,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) var priority: Priority,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: TodoStatus = TodoStatus.TODO,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) var createdAt: Instant = Instant.EPOCH
    @LastModifiedDate @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.EPOCH
}
