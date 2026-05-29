package com.tnear.adoptloop.domain

import jakarta.persistence.*

@Entity
@Table(name = "questions")
class Question(
    @Column(name = "survey_id", nullable = false) var surveyId: Long,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var type: QuestionType,
    @Column(nullable = false, columnDefinition = "TEXT") var text: String,
    @Column(name = "order_index", nullable = false) var orderIndex: Int,
    @Column(nullable = false) var required: Boolean = true,
    @Enumerated(EnumType.STRING) @Column(length = 20) var axis: Axis? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
}
