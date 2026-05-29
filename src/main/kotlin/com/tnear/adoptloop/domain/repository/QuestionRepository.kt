package com.tnear.adoptloop.domain.repository

import com.tnear.adoptloop.domain.Question
import org.springframework.data.jpa.repository.JpaRepository

interface QuestionRepository : JpaRepository<Question, Long> {
    fun findAllBySurveyIdOrderByOrderIndex(surveyId: Long): List<Question>
    fun deleteAllBySurveyId(surveyId: Long)
}
