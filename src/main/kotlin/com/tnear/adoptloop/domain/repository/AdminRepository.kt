package com.tnear.adoptloop.domain.repository

import com.tnear.adoptloop.domain.Admin
import org.springframework.data.jpa.repository.JpaRepository

interface AdminRepository : JpaRepository<Admin, Long> {
    fun findByKeyHash(keyHash: String): Admin?
}
