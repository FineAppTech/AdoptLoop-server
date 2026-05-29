package com.tnear.adoptloop.survey.draft

import com.tnear.adoptloop.config.LlmTransientException
import com.tnear.adoptloop.domain.repository.AdoptionRepository
import com.tnear.adoptloop.survey.SurveyService
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Service
class SurveyDraftService(
    private val chatClient: ChatClient,
    private val parser: SurveyDraftParser,
    private val adoptionRepository: AdoptionRepository,
    private val surveyService: SurveyService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun generate(adminId: Long, adoptionId: Long, deadline: Instant): Long {
        val adoption = adoptionRepository.findById(adoptionId)
            .orElseThrow { NoSuchElementException("adoption $adoptionId") }
        if (adoption.adminId != adminId) throw ResponseStatusException(HttpStatus.FORBIDDEN, "not owner")

        val raw = try {
            chatClient.prompt().user(SurveyDraftPrompt.build(adoption)).call().content()
                ?: throw LlmTransientException("LLM returned empty")
        } catch (e: LlmTransientException) {
            throw e
        } catch (e: Exception) {
            log.warn("LLM call failed for adoption {}: {}", adoptionId, e.message)
            throw LlmTransientException("LLM call failed: ${e.message}")
        }

        val draft = try {
            parser.parse(raw)
        } catch (e: Exception) {
            log.warn("LLM output unparseable for adoption {}: {}", adoptionId, e.message)
            throw LlmTransientException("LLM output unparseable: ${e.message}")
        }

        val survey = surveyService.createDraftWithQuestions(
            adminId, adoptionId, draft.title, deadline, draft.questions,
        )
        return survey.id!!
    }
}
