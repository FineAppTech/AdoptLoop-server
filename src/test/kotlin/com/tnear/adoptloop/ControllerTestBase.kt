package com.tnear.adoptloop

import com.tnear.adoptloop.admin.auth.AdminKeyFilter
import com.tnear.adoptloop.restdocs.RequireDocumentationExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@ExtendWith(RestDocumentationExtension::class, RequireDocumentationExtension::class)
abstract class ControllerTestBase : IntegrationTestBase() {
    @Autowired protected lateinit var context: WebApplicationContext

    // webAppContextSetup은 @AutoConfigureMockMvc와 달리 필터 빈을 자동 등록하지 않으므로
    // AdminKeyFilter를 명시 등록한다 (인증 경로 /api/admin/**). 공개 엔드포인트는 필터의
    // shouldNotFilter가 통과시킨다.
    @Autowired protected lateinit var adminKeyFilter: AdminKeyFilter

    protected lateinit var mvc: MockMvc

    @BeforeEach
    fun setUpMvc(restDocumentation: RestDocumentationContextProvider) {
        mvc = MockMvcBuilders.webAppContextSetup(context)
            .addFilters<DefaultMockMvcBuilder>(adminKeyFilter)
            .apply<DefaultMockMvcBuilder>(documentationConfiguration(restDocumentation))
            .build()
    }
}
