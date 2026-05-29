package com.tnear.adoptloop.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(name = "survey_responses")
@EntityListeners(AuditingEntityListener::class)
class SurveyResponse(
    @Column(name = "survey_id", nullable = false) var surveyId: Long,
    @Column(name = "access_token", nullable = false, unique = true) var accessToken: String,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: ResponseStatus = ResponseStatus.IN_PROGRESS,
    @Column(name = "submitted_at") var submittedAt: Instant? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) var createdAt: Instant = Instant.EPOCH
    @LastModifiedDate @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.EPOCH
}
