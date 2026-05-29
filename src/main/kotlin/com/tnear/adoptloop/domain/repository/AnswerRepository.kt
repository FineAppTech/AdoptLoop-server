package com.tnear.adoptloop.domain.repository

import com.tnear.adoptloop.domain.Answer
import org.springframework.data.jpa.repository.JpaRepository

interface AnswerRepository : JpaRepository<Answer, Long> {
    fun deleteAllBySurveyResponseId(responseId: Long)
    fun findAllBySurveyResponseId(responseId: Long): List<Answer>
    fun findAllBySurveyResponseIdIn(responseIds: Collection<Long>): List<Answer>
}
