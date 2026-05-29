package com.tnear.adoptloop.survey

import com.ninjasquad.springmockk.MockkBean
import com.tnear.adoptloop.ControllerTestBase
import com.tnear.adoptloop.domain.Admin
import com.tnear.adoptloop.domain.Adoption
import com.tnear.adoptloop.domain.repository.AdminRepository
import com.tnear.adoptloop.domain.repository.AdoptionRepository
import com.tnear.adoptloop.restdocs.documentApi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
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

class SurveyDraftServiceTest @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val adminRepository: AdminRepository,
    private val adoptionRepository: AdoptionRepository,
) : ControllerTestBase() {

    @MockkBean
    private lateinit var chatClient: ChatClient

    private fun sha256Hex(s: String) =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun seedAdmin(): Pair<String, Long> {
        val raw = "k-${System.nanoTime()}"
        val admin = adminRepository.save(Admin(name = "tester", keyHash = sha256Hex(raw)))
        return raw to admin.id!!
    }

    private fun stubChatClient(answer: ChatClient.CallResponseSpec.() -> Unit) {
        val requestSpec = mockk<ChatClient.ChatClientRequestSpec>()
        val callSpec = mockk<ChatClient.CallResponseSpec>()
        every { chatClient.prompt() } returns requestSpec
        every { requestSpec.user(any<String>()) } returns requestSpec
        every { requestSpec.call() } returns callSpec
        callSpec.answer()
    }

    private fun seedAdoption(adminId: Long) = adoptionRepository.save(Adoption(
        adminId = adminId, name = "Jira", goal = "g", targetAudience = "ta", targetCount = 10,
    ))

    private fun requestBody() =
        objectMapper.writeValueAsString(mapOf("deadline" to Instant.now().plusSeconds(3600).toString()))

    @Test
    fun `LLM JSON으로 설문 초안을 생성한다`() {
        val raw = """
            {"title":"Jira 설문","questions":[
                {"type":"SCALE","text":"사용 빈도","axis":"USAGE"},
                {"type":"SINGLE_CHOICE","text":"역할","options":["기획","개발"]}
            ]}
        """.trimIndent()
        stubChatClient { every { content() } returns raw }

        val (key, adminId) = seedAdmin()
        val adoption = seedAdoption(adminId)

        mvc.perform(post("/api/admin/adoptions/${adoption.id}/surveys")
            .header("X-Admin-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody()))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.survey.title").value("Jira 설문"))
            .andExpect(jsonPath("$.survey.status").value("DRAFT"))
            .andExpect(jsonPath("$.questions.length()").value(2))
            .andExpect(jsonPath("$.questions[1].options.length()").value(2))
            .andDo(documentApi("generate-survey-draft",
                requestFields(
                    fieldWithPath("deadline").description("응답 마감 시각 (ISO-8601)"),
                ),
                responseFields(
                    fieldWithPath("survey.id").description("설문 ID"),
                    fieldWithPath("survey.adoption_id").description("도입 ID"),
                    fieldWithPath("survey.title").description("LLM이 생성한 설문 제목"),
                    fieldWithPath("survey.public_slug").description("공개 URL slug"),
                    fieldWithPath("survey.status").description("DRAFT | PUBLISHED | CLOSED"),
                    fieldWithPath("survey.deadline").description("응답 마감 시각"),
                    fieldWithPath("survey.published_at").description("발행 시각").optional(),
                    fieldWithPath("survey.created_at").description("생성 시각"),
                    fieldWithPath("questions[].id").description("문항 ID"),
                    fieldWithPath("questions[].type").description("문항 타입 (TEXT | SINGLE_CHOICE | SCALE)"),
                    fieldWithPath("questions[].text").description("문항 본문"),
                    fieldWithPath("questions[].order_index").description("표시 순서"),
                    fieldWithPath("questions[].required").description("필수 응답 여부"),
                    fieldWithPath("questions[].axis").description("축 (USAGE | BEHAVIOR | VALUE) — SCALE 한정").optional(),
                    fieldWithPath("questions[].options").description("선택지 목록 (SINGLE_CHOICE는 항목 보유, 그 외 빈 배열)"),
                    fieldWithPath("questions[].options[].id").description("선택지 ID").optional(),
                    fieldWithPath("questions[].options[].text").description("선택지 본문").optional(),
                    fieldWithPath("questions[].options[].order_index").description("선택지 순서").optional(),
                ),
            ))
    }

    @Test
    fun `LLM 호출이 실패하면 503`() {
        stubChatClient { every { content() } throws RuntimeException("bedrock unavailable") }

        val (key, adminId) = seedAdmin()
        val adoption = seedAdoption(adminId)

        mvc.perform(post("/api/admin/adoptions/${adoption.id}/surveys")
            .header("X-Admin-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody()))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.code").value("LLM_TRANSIENT"))
            .andDo(documentApi("generate-survey-draft-llm-error",
                responseFields(
                    fieldWithPath("code").description("에러 코드 (LLM_TRANSIENT)"),
                    fieldWithPath("message").description("사용자 노출 메시지"),
                ),
            ))
    }

    @Test
    fun `마감이 과거면 400이고 LLM을 호출하지 않는다`() {
        val (key, adminId) = seedAdmin()
        val adoption = seedAdoption(adminId)

        mvc.perform(post("/api/admin/adoptions/${adoption.id}/surveys")
            .header("X-Admin-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                mapOf("deadline" to Instant.now().minusSeconds(3600).toString()))))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andDo(documentApi("generate-survey-draft-invalid-deadline",
                responseFields(
                    fieldWithPath("code").description("에러 코드 (VALIDATION_FAILED)"),
                    fieldWithPath("message").description("검증 실패 상세"),
                ),
            ))

        verify(exactly = 0) { chatClient.prompt() }
    }
}
