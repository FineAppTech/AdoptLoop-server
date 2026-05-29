package com.tnear.adoptloop.analysis

import com.tnear.adoptloop.domain.QuestionType
import com.tnear.adoptloop.domain.ResponseStatus
import com.tnear.adoptloop.domain.repository.AdoptionRepository
import com.tnear.adoptloop.domain.repository.AnswerRepository
import com.tnear.adoptloop.domain.repository.QuestionRepository
import com.tnear.adoptloop.domain.repository.SurveyRepository
import com.tnear.adoptloop.domain.repository.SurveyResponseRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
@Transactional(readOnly = true)
class AggregateService(
    private val surveyRepository: SurveyRepository,
    private val adoptionRepository: AdoptionRepository,
    private val surveyResponseRepository: SurveyResponseRepository,
    private val questionRepository: QuestionRepository,
    private val answerRepository: AnswerRepository,
) {
    fun aggregate(adminId: Long, surveyId: Long): AggregateRes {
        val survey = surveyRepository.findById(surveyId).orElseThrow { NoSuchElementException("survey") }
        val adoption = adoptionRepository.findById(survey.adoptionId).orElseThrow { NoSuchElementException("adoption") }
        if (adoption.adminId != adminId) throw ResponseStatusException(HttpStatus.FORBIDDEN, "not owner")
        val participants = surveyResponseRepository.countBySurveyIdAndStatus(surveyId, ResponseStatus.SUBMITTED).toInt()

        val questions = questionRepository.findAllBySurveyIdOrderByOrderIndex(surveyId)
        val responseIds = surveyResponseRepository.findAllBySurveyIdAndStatus(surveyId, ResponseStatus.SUBMITTED)
            .mapNotNull { it.id }
        val answers = if (responseIds.isEmpty()) emptyList()
            else answerRepository.findAllBySurveyResponseIdIn(responseIds)

        val answersByQuestion = answers.groupBy { it.questionId }
        val perQuestion = questions.map { question ->
            val questionAnswers = answersByQuestion[question.id] ?: emptyList()
            when (question.type) {
                QuestionType.TEXT -> TextAggregateVo(question.id!!, questionAnswers.mapNotNull { it.textValue })
                QuestionType.SCALE -> {
                    val scales = questionAnswers.mapNotNull { it.scaleValue }
                    ScaleAggregateVo(question.id!!, question.axis?.name,
                        average = if (scales.isEmpty()) 0.0 else scales.average(),
                        count = scales.size.toLong())
                }
                QuestionType.SINGLE_CHOICE -> {
                    val distribution = questionAnswers.mapNotNull { it.questionOptionId }
                        .groupingBy { it }.eachCount()
                        .map { (optionId, count) -> DistributionBucketVo(optionId, count.toLong()) }
                    ChoiceAggregateVo(question.id!!, distribution)
                }
            }
        }
        return AggregateRes(
            participants = participants,
            targetCount = adoption.targetCount,
            participationRate = if (adoption.targetCount == 0) 0.0 else participants.toDouble() / adoption.targetCount,
            perQuestion = perQuestion,
        )
    }
}
