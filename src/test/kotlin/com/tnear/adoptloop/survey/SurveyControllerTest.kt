package com.tnear.adoptloop.survey

import tools.jackson.databind.ObjectMapper
import com.tnear.adoptloop.ControllerTestBase
import com.tnear.adoptloop.domain.Admin
import com.tnear.adoptloop.domain.Adoption
import com.tnear.adoptloop.domain.Survey
import com.tnear.adoptloop.domain.repository.AdminRepository
import com.tnear.adoptloop.domain.repository.AdoptionRepository
import com.tnear.adoptloop.domain.repository.SurveyRepository
import com.tnear.adoptloop.restdocs.documentApi
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.security.MessageDigest
import java.time.Instant

class SurveyControllerTest @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val adminRepository: AdminRepository,
    private val adoptionRepository: AdoptionRepository,
    private val surveyRepository: SurveyRepository,
) : ControllerTestBase() {

    private fun sha256Hex(s: String) =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }

    /** admin을 시드하고 (요청용 raw 키, admin id)를 반환한다. */
    private fun seedAdmin(): Pair<String, Long> {
        val raw = "k-${System.nanoTime()}"
        val admin = adminRepository.save(Admin(name = "tester", keyHash = sha256Hex(raw)))
        return raw to admin.id!!
    }

    /** adminId 소유의 adoption 아래 DRAFT 설문을 시드한다. */
    private fun seedDraft(adminId: Long): Survey {
        val adoption = adoptionRepository.save(Adoption(
            adminId = adminId, name = "n", goal = "g", targetAudience = "ta", targetCount = 10,
        ))
        return surveyRepository.save(Survey(
            adoptionId = adoption.id!!,
            title = "t",
            publicSlug = "slug-${System.nanoTime()}",
            deadline = Instant.now().plusSeconds(3600),
        ))
    }

    private fun putQuestions(key: String, surveyId: Long, body: List<Map<String, Any>>) =
        mvc.perform(put("/api/admin/surveys/$surveyId/questions")
            .header("X-Admin-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))

    @Test
    fun `질문 없는 draft를 발행하면 409`() {
        val (key, adminId) = seedAdmin()
        val draft = seedDraft(adminId)

        mvc.perform(post("/api/admin/surveys/${draft.id}/publish")
            .header("X-Admin-Key", key))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CONFLICT"))
            .andDo(documentApi("publish-survey-empty-draft-conflict"))
    }

    @Test
    fun `PUT으로 질문을 전치환한다`() {
        val (key, adminId) = seedAdmin()
        val draft = seedDraft(adminId)

        val body = listOf(
            mapOf("type" to "TEXT", "text" to "Q1", "order_index" to 1),
            mapOf("type" to "SCALE", "text" to "Q2", "order_index" to 2, "axis" to "USAGE"),
        )
        putQuestions(key, draft.id!!, body)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.questions.length()").value(2))
            .andDo(documentApi("replace-survey-questions",
                requestFields(
                    fieldWithPath("[].type").description("문항 타입 (TEXT | SINGLE_CHOICE | SCALE)"),
                    fieldWithPath("[].text").description("문항 본문"),
                    fieldWithPath("[].order_index").description("표시 순서"),
                    fieldWithPath("[].axis").description("축 (USAGE | BEHAVIOR | VALUE) — SCALE 한정").optional(),
                ),
                responseFields(
                    fieldWithPath("survey.id").description("설문 ID"),
                    fieldWithPath("survey.adoption_id").description("도입 ID"),
                    fieldWithPath("survey.title").description("설문 제목"),
                    fieldWithPath("survey.public_slug").description("공개 URL slug"),
                    fieldWithPath("survey.status").description("DRAFT | PUBLISHED | CLOSED"),
                    fieldWithPath("survey.deadline").description("응답 마감 시각"),
                    fieldWithPath("survey.published_at").description("발행 시각").optional(),
                    fieldWithPath("survey.created_at").description("생성 시각"),
                    fieldWithPath("questions[].id").description("문항 ID"),
                    fieldWithPath("questions[].type").description("문항 타입"),
                    fieldWithPath("questions[].text").description("문항 본문"),
                    fieldWithPath("questions[].order_index").description("표시 순서"),
                    fieldWithPath("questions[].required").description("필수 응답 여부"),
                    fieldWithPath("questions[].axis").description("축 (SCALE 한정)").optional(),
                    fieldWithPath("questions[].options").description("선택지 목록 (SINGLE_CHOICE 한정)"),
                ),
            ))
    }

    @Test
    fun `질문이 있는 draft를 발행한다`() {
        val (key, adminId) = seedAdmin()
        val draft = seedDraft(adminId)
        putQuestions(key, draft.id!!, listOf(mapOf("type" to "TEXT", "text" to "Q1", "order_index" to 1)))
            .andExpect(status().isOk)

        mvc.perform(post("/api/admin/surveys/${draft.id}/publish")
            .header("X-Admin-Key", key))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.published_at").exists())
            .andDo(documentApi("publish-survey",
                responseFields(
                    fieldWithPath("id").description("설문 ID"),
                    fieldWithPath("adoption_id").description("도입 ID"),
                    fieldWithPath("title").description("설문 제목"),
                    fieldWithPath("public_slug").description("공개 URL slug"),
                    fieldWithPath("status").description("DRAFT | PUBLISHED | CLOSED"),
                    fieldWithPath("deadline").description("응답 마감 시각"),
                    fieldWithPath("published_at").description("발행 시각").optional(),
                    fieldWithPath("created_at").description("생성 시각"),
                ),
            ))
    }
}
