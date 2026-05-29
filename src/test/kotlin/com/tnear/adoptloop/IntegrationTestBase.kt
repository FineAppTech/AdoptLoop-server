package com.tnear.adoptloop

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer

@SpringBootTest
@ActiveProfiles("test")
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
        // 싱글톤 컨테이너: 한 번 start 후 JVM 종료까지 유지(Ryuk가 정리).
        // @Testcontainers/@Container의 클래스별 start/stop는 Spring 컨텍스트 캐싱과 충돌해
        // (캐시된 컨텍스트가 stop된 컨테이너 포트를 참조) 연결 거부를 유발하므로 쓰지 않는다.
        @JvmStatic
        @ServiceConnection
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("adoptloop_test")
            .withUsername("test")
            .withPassword("test")
            .also { it.start() }
    }
}
