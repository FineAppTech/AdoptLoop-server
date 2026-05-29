package com.tnear.adoptloop.domain

import jakarta.persistence.*

@Entity
@Table(name = "question_options")
class QuestionOption(
    @Column(name = "question_id", nullable = false) var questionId: Long,
    @Column(nullable = false) var text: String,
    @Column(name = "order_index", nullable = false) var orderIndex: Int,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
}
