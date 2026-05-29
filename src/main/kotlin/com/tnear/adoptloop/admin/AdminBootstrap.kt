package com.tnear.adoptloop.admin

import com.tnear.adoptloop.domain.Admin
import com.tnear.adoptloop.domain.repository.AdminRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.security.MessageDigest

@Component
class AdminBootstrap(
    private val adminRepository: AdminRepository,
    private val environment: Environment,
    @Value("\${adoptloop.admin.bootstrap-name:}") private val name: String,
    @Value("\${adoptloop.admin.bootstrap-key:}") private val key: String,
) : CommandLineRunner {

    private val log = org.slf4j.LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String) {
        if (name.isBlank() || key.isBlank()) {
            if (environment.activeProfiles.contains("prod") && adminRepository.count() == 0L) {
                log.warn("No admins exist and ADOPTLOOP_ADMIN_NAME/KEY not set — all /api/admin/* will return 401 until an admin is seeded.")
            }
            return
        }
        val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
        if (adminRepository.findByKeyHash(hash) == null) {
            adminRepository.save(Admin(name = name, keyHash = hash))
        }
    }
}
