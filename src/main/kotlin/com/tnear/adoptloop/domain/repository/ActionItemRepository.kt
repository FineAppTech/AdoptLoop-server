package com.tnear.adoptloop.domain.repository

import com.tnear.adoptloop.domain.ActionItem
import org.springframework.data.jpa.repository.JpaRepository

interface ActionItemRepository : JpaRepository<ActionItem, Long> {
    fun findAllByAdoptionId(adoptionId: Long): List<ActionItem>
}
