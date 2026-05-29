package com.tnear.adoptloop

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
    protected lateinit var mvc: MockMvc

    @BeforeEach
    fun setUpMvc(restDocumentation: RestDocumentationContextProvider) {
        mvc = MockMvcBuilders.webAppContextSetup(context)
            .apply<DefaultMockMvcBuilder>(documentationConfiguration(restDocumentation))
            .build()
    }
}
