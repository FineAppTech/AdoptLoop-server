package com.tnear.adoptloop.domain.repository

import com.tnear.adoptloop.domain.Analysis
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface AnalysisRepository : JpaRepository<Analysis, Long> {
    fun findFirstBySurveyIdOrderByCreatedAtDesc(surveyId: Long): Optional<Analysis>
    fun findAllBySurveyIdOrderByCreatedAtDesc(surveyId: Long): List<Analysis>
}
