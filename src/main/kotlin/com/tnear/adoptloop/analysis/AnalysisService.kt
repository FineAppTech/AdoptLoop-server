package com.tnear.adoptloop.analysis

import com.tnear.adoptloop.config.LlmTransientException
import com.tnear.adoptloop.domain.Analysis
import com.tnear.adoptloop.domain.repository.AdoptionRepository
import com.tnear.adoptloop.domain.repository.AnalysisRepository
import com.tnear.adoptloop.domain.repository.SurveyRepository
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import tools.jackson.databind.ObjectMapper
import java.time.Instant

// LLM 호출이 DB 트랜잭션을 붙잡지 않도록 run()은 비트랜잭션(M4 SurveyDraftService 선례).
// 분석 결과는 단일 save()의 암묵 트랜잭션으로 원자 저장되고, 엔티티에 lazy 연관이 없어 읽기도 안전하다.
@Service
class AnalysisService(
    private val aggregateService: AggregateService,
    private val chatClient: ChatClient,
    private val parser: AnalysisParser,
    private val analysisRepository: AnalysisRepository,
    private val surveyRepository: SurveyRepository,
    private val adoptionRepository: AdoptionRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun run(adminId: Long, surveyId: Long): AnalysisRunRes {
        val survey = surveyRepository.findById(surveyId).orElseThrow { NoSuchElementException("survey") }
        val adoption = adoptionRepository.findById(survey.adoptionId).orElseThrow { NoSuchElementException("adoption") }
        if (adoption.adminId != adminId) throw ResponseStatusException(HttpStatus.FORBIDDEN, "not owner")
        if (Instant.now().isBefore(survey.deadline))
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "before deadline")

        val aggregate = aggregateService.aggregate(adminId, surveyId)
        val raw = try {
            chatClient.prompt().user(AnalysisPrompt.build(adoption, aggregate, objectMapper)).call().content()
                ?: throw LlmTransientException("LLM returned empty")
        } catch (e: LlmTransientException) {
            throw e
        } catch (e: Exception) {
            log.warn("LLM call failed for survey {}: {}", surveyId, e.message)
            throw LlmTransientException("LLM call failed: ${e.message}")
        }
        val parsed = try {
            parser.parse(raw)
        } catch (e: Exception) {
            log.warn("LLM output unparseable for survey {}: {}", surveyId, e.message)
            throw LlmTransientException("LLM output unparseable: ${e.message}")
        }

        val saved = analysisRepository.save(Analysis(
            surveyId = surveyId,
            adoptionScore = parsed.scores.adoption, usageScore = parsed.scores.usage,
            behaviorScore = parsed.scores.behavior, valueScore = parsed.scores.value,
            positiveSignals = parsed.signals.positive,
            resistanceFactors = parsed.signals.resistance,
            risks = parsed.signals.risks, rawOutput = raw,
        ))
        return AnalysisRunRes(
            analysis = toView(saved),
            suggestedActionItems = parsed.suggested,
        )
    }

    @Transactional(readOnly = true)
    fun latest(adminId: Long, surveyId: Long): AnalysisRes {
        val survey = surveyRepository.findById(surveyId).orElseThrow { NoSuchElementException("survey") }
        val adoption = adoptionRepository.findById(survey.adoptionId).orElseThrow { NoSuchElementException("adoption") }
        if (adoption.adminId != adminId) throw ResponseStatusException(HttpStatus.FORBIDDEN, "not owner")
        val analysis = analysisRepository.findFirstBySurveyIdOrderByCreatedAtDesc(surveyId)
            .orElseThrow { NoSuchElementException("no analysis") }
        return toView(analysis)
    }

    private fun toView(analysis: Analysis) = AnalysisRes(
        analysis.id!!, analysis.surveyId, analysis.adoptionScore, analysis.usageScore,
        analysis.behaviorScore, analysis.valueScore,
        analysis.positiveSignals, analysis.resistanceFactors, analysis.risks, analysis.rawOutput, analysis.createdAt,
    )
}
