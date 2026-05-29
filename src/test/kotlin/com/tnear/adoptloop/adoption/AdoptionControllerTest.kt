package com.tnear.adoptloop.adoption

import tools.jackson.databind.ObjectMapper
import com.tnear.adoptloop.ControllerTestBase
import com.tnear.adoptloop.domain.Admin
import com.tnear.adoptloop.domain.Adoption
import com.tnear.adoptloop.domain.repository.AdminRepository
import com.tnear.adoptloop.domain.repository.AdoptionRepository
import com.tnear.adoptloop.restdocs.documentApi
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.security.MessageDigest

class AdoptionControllerTest @Autowired constructor(
    private val adminRepository: AdminRepository,
    private val adoptionRepository: AdoptionRepository,
    private val objectMapper: ObjectMapper,
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

    private fun seedAdoption(adminId: Long): Long =
        adoptionRepository.save(Adoption(
            adminId = adminId, name = "n", goal = "g", targetAudience = "ta", targetCount = 10,
        )).id!!

    @Test
    fun `도입을 생성한다`() {
        val (key, _) = seedAdmin()
        val body = mapOf(
            "name" to "Jira 도입",
            "goal" to "협업 가시화",
            "target_audience" to "전사 50명",
            "target_count" to 50,
        )
        mvc.perform(
            post("/api/admin/adoptions")
                .header("X-Admin-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Jira 도입"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andDo(documentApi("create-adoption",
                requestFields(
                    fieldWithPath("name").description("도입 이름"),
                    fieldWithPath("goal").description("도입 목적"),
                    fieldWithPath("target_audience").description("대상"),
                    fieldWithPath("target_count").description("대상 인원수"),
                ),
                responseFields(
                    fieldWithPath("id").description("도입 ID"),
                    fieldWithPath("admin_id").description("소유 admin ID"),
                    fieldWithPath("name").description("도입 이름"),
                    fieldWithPath("goal").description("도입 목적"),
                    fieldWithPath("target_audience").description("대상"),
                    fieldWithPath("concern").description("우려/제약").optional(),
                    fieldWithPath("target_count").description("대상 인원수"),
                    fieldWithPath("status").description("ACTIVE | ARCHIVED"),
                    fieldWithPath("created_at").description("생성 시각"),
                    fieldWithPath("updated_at").description("수정 시각"),
                ),
            ))
    }

    @Test
    fun `target_count가 1 미만이면 400`() {
        val (key, _) = seedAdmin()
        val body = mapOf("target_count" to 0)
        mvc.perform(
            patch("/api/admin/adoptions/1")
                .header("X-Admin-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andDo(documentApi("update-adoption-invalid",
                responseFields(
                    fieldWithPath("code").description("에러 코드"),
                    fieldWithPath("message").description("에러 메시지"),
                ),
            ))
    }

    @Test
    fun `남의 도입을 조회하면 403`() {
        val (_, ownerId) = seedAdmin()
        val (otherKey, _) = seedAdmin()
        val adoptionId = seedAdoption(ownerId)

        mvc.perform(
            get("/api/admin/adoptions/$adoptionId").header("X-Admin-Key", otherKey)
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andDo(documentApi("get-adoption-forbidden",
                responseFields(
                    fieldWithPath("code").description("에러 코드"),
                    fieldWithPath("message").description("에러 메시지"),
                ),
            ))
    }
}
