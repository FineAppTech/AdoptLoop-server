package com.tnear.adoptloop.admin

import com.tnear.adoptloop.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AdminKeyFilterTest @Autowired constructor(
    private val mvc: MockMvc,
) : IntegrationTestBase() {

    @Test
    fun `missing key returns 401`() {
        mvc.perform(get("/api/admin/adoptions"))
            .andExpect(status().isUnauthorized())
    }

    @Test
    fun `invalid key returns 401`() {
        mvc.perform(get("/api/admin/adoptions").header("X-Admin-Key", "no-such-key"))
            .andExpect(status().isUnauthorized())
    }
}
