package com.tnear.adoptloop.domain

import com.tnear.adoptloop.IntegrationTestBase
import com.tnear.adoptloop.domain.repository.AdminRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdminRepositoryTest @Autowired constructor(
    private val repo: AdminRepository,
) : IntegrationTestBase() {

    @Test
    fun `save and findByKeyHash`() {
        val saved = repo.save(Admin(name = "alice", keyHash = "a".repeat(64)))
        assertNotNull(saved.id)
        assertTrue(saved.createdAt.isAfter(Instant.EPOCH), "createdAt should be populated by JPA auditing")

        val found = repo.findByKeyHash("a".repeat(64))
        assertEquals("alice", found?.name)
    }
}
