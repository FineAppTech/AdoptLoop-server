package com.tnear.adoptloop.domain.repository

import com.tnear.adoptloop.domain.ResponseStatus
import com.tnear.adoptloop.domain.SurveyResponse
import org.springframework.data.jpa.repository.JpaRepository

interface SurveyResponseRepository : JpaRepository<SurveyResponse, Long> {
    fun findByAccessToken(token: String): SurveyResponse?
    fun countBySurveyIdAndStatus(surveyId: Long, status: ResponseStatus): Long
    fun countBySurveyId(surveyId: Long): Long
    fun findAllBySurveyIdAndStatus(surveyId: Long, status: ResponseStatus): List<SurveyResponse>
}
