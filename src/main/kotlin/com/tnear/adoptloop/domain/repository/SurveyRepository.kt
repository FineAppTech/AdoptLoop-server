package com.tnear.adoptloop.domain.repository

import com.tnear.adoptloop.domain.Survey
import org.springframework.data.jpa.repository.JpaRepository

interface SurveyRepository : JpaRepository<Survey, Long> {
    fun findByPublicSlug(slug: String): Survey?
    fun findAllByAdoptionId(adoptionId: Long): List<Survey>
}
