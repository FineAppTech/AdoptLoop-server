package com.tnear.adoptloop.publicapi

import com.tnear.adoptloop.ControllerTestBase
import com.tnear.adoptloop.domain.Admin
import com.tnear.adoptloop.domain.Adoption
import com.tnear.adoptloop.domain.Question
import com.tnear.adoptloop.domain.QuestionOption
import com.tnear.adoptloop.domain.QuestionType
import com.tnear.adoptloop.domain.Survey
import com.tnear.adoptloop.domain.SurveyStatus
import com.tnear.adoptloop.domain.repository.AdminRepository
import com.tnear.adoptloop.domain.repository.AdoptionRepository
import com.tnear.adoptloop.domain.repository.QuestionOptionRepository
import com.tnear.adoptloop.domain.repository.QuestionRepository
import com.tnear.adoptloop.domain.repository.SurveyRepository
import com.tnear.adoptloop.restdocs.documentApi
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.Instant

class PublicSurveyControllerTest @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val adoptionRepository: AdoptionRepository,
    private val adminRepository: AdminRepository,
    private val surveyRepository: SurveyRepository,
    private val questionRepository: QuestionRepository,
    private val questionOptionRepository: QuestionOptionRepository,
) : ControllerTestBase() {

    @Test
    fun `토큰 발급 후 답변을 제출하면 재로드 시 제출된 답변이 보인다`() {
        val survey = seedPublishedSurveyWithTextQuestion()

        // 1. 토큰 발급
        val tokenJson = mvc.perform(post("/api/public/surveys/${survey.publicSlug}/responses"))
            .andExpect(status().isCreated)
            .andDo(documentApi("start-public-response",
                responseFields(
                    fieldWithPath("access_token").description("응답 토큰 (이후 답변 PUT/GET에 사용)"),
                ),
            ))
            .andReturn().response.contentAsString
        val token = objectMapper.readTree(tokenJson)["access_token"].asString()

        // 2. 답변 제출
        val question = questionRepository.findAllBySurveyIdOrderByOrderIndex(survey.id!!).first()
        val body = listOf(mapOf("question_id" to question.id, "text_value" to "good"))
        mvc.perform(put("/api/public/responses/$token/answers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUBMITTED"))
            .andExpect(jsonPath("$.answers[0].text_value").value("good"))
            .andDo(documentApi("submit-public-response",
                requestFields(
                    fieldWithPath("[].question_id").description("문항 ID"),
                    fieldWithPath("[].text_value").description("주관식 답변 (TEXT 한정)").optional(),
                    fieldWithPath("[].question_option_id").type(JsonFieldType.NUMBER).description("선택지 ID (SINGLE_CHOICE 한정)").optional(),
                    fieldWithPath("[].scale_value").type(JsonFieldType.NUMBER).description("척도 점수 1-5 (SCALE 한정)").optional(),
                ),
                responseFields(
                    fieldWithPath("survey.title").description("설문 제목"),
                    fieldWithPath("survey.deadline").description("응답 마감 시각"),
                    fieldWithPath("survey.questions[].id").description("문항 ID"),
                    fieldWithPath("survey.questions[].type").description("문항 타입"),
                    fieldWithPath("survey.questions[].text").description("문항 본문"),
                    fieldWithPath("survey.questions[].order_index").description("표시 순서"),
                    fieldWithPath("survey.questions[].required").description("필수 응답 여부"),
                    fieldWithPath("survey.questions[].axis").description("축 (SCALE 한정)").optional(),
                    fieldWithPath("survey.questions[].options").description("선택지 목록 (SINGLE_CHOICE 한정)"),
                    fieldWithPath("status").description("IN_PROGRESS | SUBMITTED"),
                    fieldWithPath("submitted_at").description("제출 시각").optional(),
                    fieldWithPath("answers[].question_id").description("문항 ID"),
                    fieldWithPath("answers[].text_value").description("주관식 답변").optional(),
                    fieldWithPath("answers[].question_option_id").description("선택지 ID").optional(),
                    fieldWithPath("answers[].scale_value").description("척도 점수").optional(),
                ),
            ))
    }

    @Test
    fun `잘못된 토큰으로 제출하면 401`() {
        mvc.perform(put("/api/public/responses/bogus/answers")
            .contentType(MediaType.APPLICATION_JSON)
            .content("[]"))
            .andExpect(status().isUnauthorized)
            .andDo(documentApi("submit-public-response-invalid-token"))
    }

    @Test
    fun `마감이 지난 설문 조회는 410`() {
        val survey = seedPublishedSurveyWithTextQuestion(deadline = Instant.now().minusSeconds(60))
        mvc.perform(get("/api/public/surveys/${survey.publicSlug}"))
            .andExpect(status().isGone)
            .andDo(documentApi("get-public-survey-deadline-passed"))
    }

    @Test
    fun `해당 문항에 속하지 않는 선택지로 제출하면 400`() {
        val survey = seedPublishedSurveyWithSingleChoiceQuestion()
        val token = objectMapper.readTree(
            mvc.perform(post("/api/public/surveys/${survey.publicSlug}/responses"))
                .andExpect(status().isCreated)
                .andReturn().response.contentAsString,
        )["access_token"].asString()

        val question = questionRepository.findAllBySurveyIdOrderByOrderIndex(survey.id!!).first()
        val body = listOf(mapOf("question_id" to question.id, "question_option_id" to 999_999))
        mvc.perform(put("/api/public/responses/$token/answers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest)
            .andDo(documentApi("submit-public-response-invalid-option"))
    }

    private fun seedPublishedSurveyWithTextQuestion(deadline: Instant = Instant.now().plusSeconds(3600)): Survey {
        val admin = adminRepository.save(Admin(name = "x", keyHash = "h${System.nanoTime()}".padEnd(64, '0').take(64)))
        val adoption = adoptionRepository.save(Adoption(admin.id!!, "n", "g", "ta", null, 10))
        val survey = surveyRepository.save(Survey(adoption.id!!, "t", "slug-${System.nanoTime()}",
            status = SurveyStatus.PUBLISHED, deadline = deadline, publishedAt = Instant.now()))
        questionRepository.save(Question(survey.id!!, QuestionType.TEXT, "Q1", 1, true, null))
        return survey
    }

    private fun seedPublishedSurveyWithSingleChoiceQuestion(): Survey {
        val admin = adminRepository.save(Admin(name = "x", keyHash = "h${System.nanoTime()}".padEnd(64, '0').take(64)))
        val adoption = adoptionRepository.save(Adoption(admin.id!!, "n", "g", "ta", null, 10))
        val survey = surveyRepository.save(Survey(adoption.id!!, "t", "slug-${System.nanoTime()}",
            status = SurveyStatus.PUBLISHED, deadline = Instant.now().plusSeconds(3600), publishedAt = Instant.now()))
        val question = questionRepository.save(Question(survey.id!!, QuestionType.SINGLE_CHOICE, "Q1", 1, true, null))
        questionOptionRepository.save(QuestionOption(question.id!!, "option A", 1))
        return survey
    }
}
