package com.tnear.adoptloop.domain

import jakarta.persistence.*

@Entity
@Table(name = "answers")
class Answer(
    @Column(name = "survey_response_id", nullable = false) var surveyResponseId: Long,
    @Column(name = "question_id", nullable = false) var questionId: Long,
    @Column(name = "text_value", columnDefinition = "TEXT") var textValue: String? = null,
    @Column(name = "question_option_id") var questionOptionId: Long? = null,
    @Column(name = "scale_value") var scaleValue: Int? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
}
