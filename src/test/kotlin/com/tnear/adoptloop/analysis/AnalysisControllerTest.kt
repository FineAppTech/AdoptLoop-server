package com.tnear.adoptloop.analysis

import com.tnear.adoptloop.ControllerTestBase
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
import com.tnear.adoptloop.restdocs.documentApi
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.security.MessageDigest
import java.time.Instant

class AnalysisControllerTest @Autowired constructor(
    private val adminRepository: AdminRepository,
    private val adoptionRepository: AdoptionRepository,
    private val surveyRepository: SurveyRepository,
    private val questionRepository: QuestionRepository,
    private val surveyResponseRepository: SurveyResponseRepository,
    private val answerRepository: AnswerRepository,
) : ControllerTestBase() {

    @Test
    fun `aggregate는 참여율과 SCALE 문항 집계를 반환한다`() {
        val (key, adminId) = seedAdmin()
        val adoption = adoptionRepository.save(Adoption(adminId, "n", "g", "ta", null, 10))
        val survey = surveyRepository.save(Survey(adoption.id!!, "t", "slug-${System.nanoTime()}",
            status = SurveyStatus.PUBLISHED, deadline = Instant.now().plusSeconds(60)))
        val question = questionRepository.save(Question(survey.id!!, QuestionType.SCALE, "Q", 1, true, Axis.USAGE))
        listOf(3, 4, 5).forEach { value ->
            val response = surveyResponseRepository.save(SurveyResponse(survey.id!!, "tk${System.nanoTime()}",
                status = ResponseStatus.SUBMITTED, submittedAt = Instant.now()))
            answerRepository.save(Answer(response.id!!, question.id!!, scaleValue = value))
        }

        mvc.perform(get("/api/admin/surveys/${survey.id}/aggregate")
            .header("X-Admin-Key", key))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.participants").value(3))
            .andDo(documentApi("get-survey-aggregate",
                responseFields(
                    fieldWithPath("participants").description("제출 완료 응답자 수"),
                    fieldWithPath("target_count").description("도입 대상 인원수"),
                    fieldWithPath("participation_rate").description("참여율 (0.0 ~ 1.0)"),
                    fieldWithPath("per_question[].question_id").description("문항 ID"),
                    fieldWithPath("per_question[].type").description("SCALE | SINGLE_CHOICE | TEXT"),
                    fieldWithPath("per_question[].axis").description("축 (SCALE 한정)"),
                    fieldWithPath("per_question[].average").description("평균 점수 (SCALE 한정)"),
                    fieldWithPath("per_question[].count").description("응답 수 (SCALE 한정)"),
                ),
            ))
    }

    @Test
    fun `남의 설문 집계를 조회하면 403`() {
        val (attackerKey, _) = seedAdmin()
        val (_, ownerId) = seedAdmin()
        val adoption = adoptionRepository.save(Adoption(ownerId, "n", "g", "ta", null, 10))
        val survey = surveyRepository.save(Survey(adoption.id!!, "t", "slug-${System.nanoTime()}",
            status = SurveyStatus.PUBLISHED, deadline = Instant.now().plusSeconds(60)))

        mvc.perform(get("/api/admin/surveys/${survey.id}/aggregate")
            .header("X-Admin-Key", attackerKey))
            .andExpect(status().isForbidden)
            .andDo(documentApi("get-survey-aggregate-forbidden"))
    }

    private fun seedAdmin(): Pair<String, Long> {
        val raw = "k-${System.nanoTime()}"
        val hash = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val saved = adminRepository.save(Admin(name = "t", keyHash = hash))
        return raw to saved.id!!
    }
}
