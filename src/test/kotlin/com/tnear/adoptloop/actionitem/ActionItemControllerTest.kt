package com.tnear.adoptloop.actionitem

import com.tnear.adoptloop.ControllerTestBase
import com.tnear.adoptloop.domain.Admin
import com.tnear.adoptloop.domain.Adoption
import com.tnear.adoptloop.domain.Analysis
import com.tnear.adoptloop.domain.Survey
import com.tnear.adoptloop.domain.SurveyStatus
import com.tnear.adoptloop.domain.repository.AdminRepository
import com.tnear.adoptloop.domain.repository.AdoptionRepository
import com.tnear.adoptloop.domain.repository.AnalysisRepository
import com.tnear.adoptloop.domain.repository.SurveyRepository
import com.tnear.adoptloop.restdocs.documentApi
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.security.MessageDigest
import java.time.Instant

class ActionItemControllerTest @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val adminRepository: AdminRepository,
    private val adoptionRepository: AdoptionRepository,
    private val surveyRepository: SurveyRepository,
    private val analysisRepository: AnalysisRepository,
) : ControllerTestBase() {

    @Test
    fun `analysis로부터 액션 아이템을 채택한다`() {
        val (key, adminId) = seedAdmin()
        val adoption = adoptionRepository.save(Adoption(adminId, "n", "g", "ta", null, 10))
        val survey = surveyRepository.save(Survey(adoption.id!!, "t", "slug-${System.nanoTime()}",
            status = SurveyStatus.PUBLISHED, deadline = Instant.now().plusSeconds(60)))
        val analysis = analysisRepository.save(Analysis(
            surveyId = survey.id!!,
            adoptionScore = 70, usageScore = 60, behaviorScore = 65, valueScore = 80,
            positiveSignals = listOf("A"), resistanceFactors = listOf("B"), risks = listOf("C"),
            rawOutput = "{}",
        ))

        val body = listOf(mapOf(
            "analysis_id" to analysis.id,
            "title" to "사용성 가이드 작성",
            "description" to "신규 사용자용 온보딩 가이드 페이지를 작성한다",
            "priority" to "HIGH",
        ))
        mvc.perform(post("/api/admin/adoptions/${adoption.id}/action-items")
            .header("X-Admin-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$[0].status").value("TODO"))
            .andDo(documentApi("adopt-action-items",
                requestFields(
                    fieldWithPath("[].analysis_id").description("출처 분석 ID"),
                    fieldWithPath("[].title").description("액션 제목"),
                    fieldWithPath("[].description").description("상세 설명").optional(),
                    fieldWithPath("[].priority").description("HIGH | MEDIUM | LOW"),
                ),
                responseFields(
                    fieldWithPath("[].id").description("액션 ID"),
                    fieldWithPath("[].adoption_id").description("도입 ID"),
                    fieldWithPath("[].analysis_id").description("출처 분석 ID"),
                    fieldWithPath("[].title").description("액션 제목"),
                    fieldWithPath("[].description").description("상세 설명").optional(),
                    fieldWithPath("[].priority").description("HIGH | MEDIUM | LOW"),
                    fieldWithPath("[].status").description("TODO | IN_PROGRESS | DONE"),
                    fieldWithPath("[].created_at").description("생성 시각"),
                    fieldWithPath("[].updated_at").description("수정 시각"),
                ),
            ))
    }

    private fun seedAdmin(): Pair<String, Long> {
        val raw = "k-${System.nanoTime()}"
        val hash = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val saved = adminRepository.save(Admin(name = "t", keyHash = hash))
        return raw to saved.id!!
    }
}
