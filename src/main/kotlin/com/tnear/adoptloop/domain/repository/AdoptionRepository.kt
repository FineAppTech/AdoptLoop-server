package com.tnear.adoptloop.domain.repository

import com.tnear.adoptloop.domain.Adoption
import org.springframework.data.jpa.repository.JpaRepository

interface AdoptionRepository : JpaRepository<Adoption, Long> {
    fun findAllByAdminId(adminId: Long): List<Adoption>
}
