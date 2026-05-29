package com.tnear.adoptloop.domain.repository

import com.tnear.adoptloop.domain.QuestionOption
import org.springframework.data.jpa.repository.JpaRepository

interface QuestionOptionRepository : JpaRepository<QuestionOption, Long> {
    fun findAllByQuestionIdInOrderByOrderIndex(questionIds: Collection<Long>): List<QuestionOption>
    fun deleteAllByQuestionIdIn(questionIds: Collection<Long>)
}
