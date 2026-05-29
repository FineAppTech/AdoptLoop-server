package com.tnear.adoptloop.actionitem

import com.tnear.adoptloop.domain.ActionItem
import com.tnear.adoptloop.domain.repository.ActionItemRepository
import com.tnear.adoptloop.domain.repository.AdoptionRepository
import com.tnear.adoptloop.domain.repository.AnalysisRepository
import com.tnear.adoptloop.domain.repository.SurveyRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
@Transactional
class ActionItemService(
    private val actionItemRepository: ActionItemRepository,
    private val adoptionRepository: AdoptionRepository,
    private val analysisRepository: AnalysisRepository,
    private val surveyRepository: SurveyRepository,
) {
    fun adopt(adminId: Long, adoptionId: Long, items: List<ActionItemCreateReq>): List<ActionItem> {
        val adoption = adoptionRepository.findById(adoptionId).orElseThrow { NoSuchElementException("adoption") }
        if (adoption.adminId != adminId) throw ResponseStatusException(HttpStatus.FORBIDDEN, "not owner")
        return items.map { item ->
            val analysis = analysisRepository.findById(item.analysisId).orElseThrow { NoSuchElementException("analysis") }
            val survey = surveyRepository.findById(analysis.surveyId).orElseThrow { NoSuchElementException("survey") }
            if (survey.adoptionId != adoptionId)
                throw ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "analysis ${item.analysisId} does not belong to adoption $adoptionId")
            actionItemRepository.save(ActionItem(
                adoptionId = adoptionId, analysisId = item.analysisId,
                title = item.title, description = item.description, priority = item.priority,
            ))
        }
    }

    @Transactional(readOnly = true)
    fun list(adminId: Long, adoptionId: Long): List<ActionItem> {
        val adoption = adoptionRepository.findById(adoptionId).orElseThrow { NoSuchElementException("adoption") }
        if (adoption.adminId != adminId) throw ResponseStatusException(HttpStatus.FORBIDDEN, "not owner")
        return actionItemRepository.findAllByAdoptionId(adoptionId)
    }

    fun updateStatus(adminId: Long, id: Long, req: ActionItemUpdateReq): ActionItem {
        val item = actionItemRepository.findById(id).orElseThrow { NoSuchElementException("action item") }
        val adoption = adoptionRepository.findById(item.adoptionId).orElseThrow { NoSuchElementException("adoption") }
        if (adoption.adminId != adminId) throw ResponseStatusException(HttpStatus.FORBIDDEN, "not owner")
        req.status?.let { item.status = it }
        return item
    }
}
