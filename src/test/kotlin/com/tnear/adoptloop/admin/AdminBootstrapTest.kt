package com.tnear.adoptloop.admin

import com.tnear.adoptloop.IntegrationTestBase
import com.tnear.adoptloop.domain.repository.AdminRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import java.security.MessageDigest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AdminBootstrapTest @Autowired constructor(
    private val adminRepository: AdminRepository,
    private val environment: Environment,
) : IntegrationTestBase() {

    private fun sha256Hex(s: String) =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }

    @Test
    fun `seeds admin from env name and key`() {
        AdminBootstrap(adminRepository, environment, "tester", "devkey").run()

        val admin = adminRepository.findByKeyHash(sha256Hex("devkey"))
        assertNotNull(admin)
        assertEquals("tester", admin.name)
    }

    @Test
    fun `is idempotent across runs`() {
        val bootstrap = AdminBootstrap(adminRepository, environment, "tester", "devkey")
        bootstrap.run()
        bootstrap.run()

        assertEquals(1, adminRepository.count())
    }

    @Test
    fun `blank key is a no-op`() {
        AdminBootstrap(adminRepository, environment, "tester", "").run()

        assertEquals(0, adminRepository.count())
    }
}
