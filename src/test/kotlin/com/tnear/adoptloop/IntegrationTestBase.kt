package com.tnear.adoptloop

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
abstract class IntegrationTestBase {

    @Autowired
    protected lateinit var jdbc: JdbcTemplate

    @BeforeEach
    fun truncateAll() {
        jdbc.execute("""
            TRUNCATE TABLE
              answers, action_items, analyses, survey_responses,
              question_options, questions, surveys, adoptions, admins
            RESTART IDENTITY CASCADE
        """.trimIndent())
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("adoptloop_test")
            .withUsername("test")
            .withPassword("test")
    }
}
