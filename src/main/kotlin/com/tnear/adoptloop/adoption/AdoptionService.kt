package com.tnear.adoptloop.adoption

import com.tnear.adoptloop.domain.Adoption
import com.tnear.adoptloop.domain.repository.AdoptionRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
@Transactional
class AdoptionService(private val adoptionRepository: AdoptionRepository) {

    fun create(adminId: Long, req: AdoptionCreateReq): Adoption =
        adoptionRepository.save(Adoption(
            adminId = adminId,
            name = req.name,
            goal = req.goal,
            targetAudience = req.targetAudience,
            concern = req.concern,
            targetCount = req.targetCount,
        ))

    @Transactional(readOnly = true)
    fun listForAdmin(adminId: Long): List<Adoption> = adoptionRepository.findAllByAdminId(adminId)

    @Transactional(readOnly = true)
    fun get(adminId: Long, id: Long): Adoption {
        val adoption = adoptionRepository.findById(id).orElseThrow { NoSuchElementException("adoption $id") }
        if (adoption.adminId != adminId) throw ResponseStatusException(HttpStatus.FORBIDDEN, "not owner")
        return adoption
    }

    fun update(adminId: Long, id: Long, req: AdoptionUpdateReq): Adoption {
        val adoption = get(adminId, id)
        req.name?.let { adoption.name = it }
        req.goal?.let { adoption.goal = it }
        req.targetAudience?.let { adoption.targetAudience = it }
        req.concern?.let { adoption.concern = it }
        req.targetCount?.let { adoption.targetCount = it }
        req.status?.let { adoption.status = it }
        return adoption
    }
}
