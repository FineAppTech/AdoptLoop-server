package com.tnear.adoptloop.web

import com.tnear.adoptloop.IntegrationTestBase
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

// view 컨트롤러는 REST 엔드포인트가 아니므로 ControllerTestBase(문서화 강제) 대신 IntegrationTestBase로 둔다.
// webAppContextSetup으로 GET하면 Thymeleaf가 실제 렌더 → 템플릿 파싱 오류를 헤드리스로 잡는다.
class WebPagesSmokeTest @Autowired constructor(
    private val context: WebApplicationContext,
) : IntegrationTestBase() {

    private val mvc: MockMvc by lazy { MockMvcBuilders.webAppContextSetup(context).build() }

    @Test
    fun `관리자 페이지들이 200으로 렌더된다`() {
        listOf(
            "/admin/login",
            "/admin/adoptions",
            "/admin/adoptions/new",
            "/admin/adoptions/1",
            "/admin/surveys/1/edit",
            "/admin/surveys/1/analyze",
        ).forEach { path ->
            mvc.perform(get(path)).andExpect(status().isOk)
        }
    }

    @Test
    fun `응답자 설문 페이지가 200으로 렌더된다`() {
        mvc.perform(get("/s/sample-slug"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("/js/respondent.js")))
    }

    @Test
    fun `admin 루트는 도입 목록으로 리다이렉트한다`() {
        mvc.perform(get("/admin"))
            .andExpect(status().is3xxRedirection)
    }
}
