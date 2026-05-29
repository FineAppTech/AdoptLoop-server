package com.tnear.adoptloop.analysis

import com.tnear.adoptloop.IntegrationTestBase
import com.tnear.adoptloop.domain.Admin
import com.tnear.adoptloop.domain.Adoption
import com.tnear.adoptloop.domain.Answer
import com.tnear.adoptloop.domain.Axis
import com.tnear.adoptloop.domain.Question
import com.tnear.adoptloop.domain.QuestionType
import com.tnear.adoptloop.domain.ResponseStatus
import com.tnear.adoptloop.domain.Survey
import com.tnear.adoptloop.domain.SurveyResponse
import com.tnear.adoptloop.domain.SurveyStatus
import com.tnear.adoptloop.domain.repository.AdminRepository
import com.tnear.adoptloop.domain.repository.AdoptionRepository
import com.tnear.adoptloop.domain.repository.AnswerRepository
import com.tnear.adoptloop.domain.repository.QuestionRepository
import com.tnear.adoptloop.domain.repository.SurveyRepository
import com.tnear.adoptloop.domain.repository.SurveyResponseRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import kotlin.test.assertEquals

class AggregateServiceTest @Autowired constructor(
    private val service: AggregateService,
    private val adminRepository: AdminRepository,
    private val adoptionRepository: AdoptionRepository,
    private val surveyRepository: SurveyRepository,
    private val questionRepository: QuestionRepository,
    private val surveyResponseRepository: SurveyResponseRepository,
    private val answerRepository: AnswerRepository,
) : IntegrationTestBase() {

    @Test
    fun `SCALE 평균과 참여율을 집계한다`() {
        val adoption = setupAdoption(targetCount = 10)
        val survey = surveyRepository.save(Survey(adoption.id!!, "t", "slug-${System.nanoTime()}",
            status = SurveyStatus.PUBLISHED, deadline = Instant.now().plusSeconds(60)))
        val question = questionRepository.save(Question(survey.id!!, QuestionType.SCALE, "Q", 1, true, Axis.USAGE))
        listOf(3, 4, 5).forEach { value ->
            val response = surveyResponseRepository.save(SurveyResponse(survey.id!!, "tk${System.nanoTime()}",
                status = ResponseStatus.SUBMITTED, submittedAt = Instant.now()))
            answerRepository.save(Answer(response.id!!, question.id!!, scaleValue = value))
        }

        val aggregate = service.aggregate(adoption.adminId, survey.id!!)
        assertEquals(3, aggregate.participants)
        assertEquals(0.3, aggregate.participationRate)
        val scaleAggregate = aggregate.perQuestion.first() as ScaleAggregateVo
        assertEquals(4.0, scaleAggregate.average)
    }

    private fun setupAdoption(targetCount: Int) =
        adoptionRepository.save(Adoption(
            adminId = adminRepository.save(Admin(name = "x", keyHash = "h${System.nanoTime()}".padEnd(64, '0').take(64))).id!!,
            name = "n", goal = "g", targetAudience = "ta", concern = null, targetCount = targetCount,
        ))
}
