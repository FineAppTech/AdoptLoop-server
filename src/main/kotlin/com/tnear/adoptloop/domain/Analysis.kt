package com.tnear.adoptloop.domain

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(name = "analyses")
@EntityListeners(AuditingEntityListener::class)
class Analysis(
    @Column(name = "survey_id", nullable = false) var surveyId: Long,
    @Column(name = "adoption_score", nullable = false) var adoptionScore: Int,
    @Column(name = "usage_score", nullable = false) var usageScore: Int,
    @Column(name = "behavior_score", nullable = false) var behaviorScore: Int,
    @Column(name = "value_score", nullable = false) var valueScore: Int,
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "positive_signals", nullable = false, columnDefinition = "jsonb")
    var positiveSignals: List<String>,
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "resistance_factors", nullable = false, columnDefinition = "jsonb")
    var resistanceFactors: List<String>,
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb")
    var risks: List<String>,
    @Column(name = "raw_output", nullable = false, columnDefinition = "TEXT") var rawOutput: String,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) var createdAt: Instant = Instant.EPOCH
}
