package com.tnear.adoptloop.adoption

import com.tnear.adoptloop.domain.Adoption
import com.tnear.adoptloop.domain.AdoptionStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class AdoptionCreateReq(
    @field:NotBlank val name: String,
    @field:NotBlank val goal: String,
    @field:NotBlank val targetAudience: String,
    val concern: String? = null,
    @field:Min(1) val targetCount: Int,
)

data class AdoptionUpdateReq(
    val name: String? = null,
    val goal: String? = null,
    val targetAudience: String? = null,
    val concern: String? = null,
    @field:Min(1) val targetCount: Int? = null,
    val status: AdoptionStatus? = null,
)

data class AdoptionRes(
    val id: Long,
    val adminId: Long,
    val name: String,
    val goal: String,
    val targetAudience: String,
    val concern: String?,
    val targetCount: Int,
    val status: AdoptionStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(adoption: Adoption) = AdoptionRes(
            adoption.id!!, adoption.adminId, adoption.name, adoption.goal, adoption.targetAudience,
            adoption.concern, adoption.targetCount, adoption.status, adoption.createdAt, adoption.updatedAt,
        )
    }
}
