package com.tnear.adoptloop.publicapi

import com.tnear.adoptloop.domain.Answer
import com.tnear.adoptloop.domain.QuestionType
import com.tnear.adoptloop.domain.ResponseStatus
import com.tnear.adoptloop.domain.Survey
import com.tnear.adoptloop.domain.SurveyResponse
import com.tnear.adoptloop.domain.SurveyStatus
import com.tnear.adoptloop.domain.repository.AdoptionRepository
import com.tnear.adoptloop.domain.repository.AnswerRepository
import com.tnear.adoptloop.domain.repository.QuestionOptionRepository
import com.tnear.adoptloop.domain.repository.QuestionRepository
import com.tnear.adoptloop.domain.repository.SurveyRepository
import com.tnear.adoptloop.domain.repository.SurveyResponseRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

@Service
@Transactional
class PublicResponseService(
    private val surveyRepository: SurveyRepository,
    private val surveyResponseRepository: SurveyResponseRepository,
    private val answerRepository: AnswerRepository,
    private val questionRepository: QuestionRepository,
    private val questionOptionRepository: QuestionOptionRepository,
    private val adoptionRepository: AdoptionRepository,
) {
    private val random = SecureRandom()

    @Transactional(readOnly = true)
    fun loadBySlug(slug: String): Survey {
        val survey = surveyRepository.findByPublicSlug(slug)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "survey")
        if (survey.status != SurveyStatus.PUBLISHED)
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "draft survey")
        if (Instant.now().isAfter(survey.deadline))
            throw ResponseStatusException(HttpStatus.GONE, "deadline passed")
        return survey
    }

    fun startResponse(slug: String): SurveyResponse {
        val survey = loadBySlug(slug)
        val adoption = adoptionRepository.findById(survey.adoptionId)
            .orElseThrow { NoSuchElementException("adoption") }
        val responseCap = adoption.targetCount * 10L
        if (surveyResponseRepository.countBySurveyId(survey.id!!) >= responseCap)
            throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "response limit reached")
        return surveyResponseRepository.save(SurveyResponse(surveyId = survey.id!!, accessToken = newToken()))
    }

    @Transactional(readOnly = true)
    fun loadByToken(token: String): Pair<SurveyResponse, Survey> {
        val response = surveyResponseRepository.findByAccessToken(token)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "token")
        val survey = surveyRepository.findById(response.surveyId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "survey") }
        return response to survey
    }

    fun submit(token: String, inputs: List<AnswerReq>): SurveyResponse {
        val (response, survey) = loadByToken(token)
        if (Instant.now().isAfter(survey.deadline))
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "deadline")

        val questions = questionRepository.findAllBySurveyIdOrderByOrderIndex(survey.id!!).associateBy { it.id!! }
        val optionIdsByQuestion = if (questions.isEmpty()) emptyMap()
            else questionOptionRepository.findAllByQuestionIdInOrderByOrderIndex(questions.keys)
                .groupBy { it.questionId }
                .mapValues { (_, options) -> options.mapNotNull { it.id }.toSet() }
        val requiredIds = questions.values.filter { it.required }.mapNotNull { it.id }.toSet()
        val answeredIds = inputs.map { it.questionId }.toSet()
        val missing = requiredIds - answeredIds
        if (missing.isNotEmpty())
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "missing required questions: $missing")

        answerRepository.deleteAllBySurveyResponseId(response.id!!)
        answerRepository.flush()
        inputs.forEach { input ->
            val question = questions[input.questionId]
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown question ${input.questionId}")
            validateMatchesType(question.type, input, optionIdsByQuestion[input.questionId].orEmpty())
            answerRepository.save(Answer(
                surveyResponseId = response.id!!, questionId = input.questionId,
                textValue = input.textValue, questionOptionId = input.questionOptionId, scaleValue = input.scaleValue,
            ))
        }
        if (response.status == ResponseStatus.IN_PROGRESS) {
            response.status = ResponseStatus.SUBMITTED
            response.submittedAt = Instant.now()
        }
        return response
    }

    fun loadAnswers(responseId: Long): List<Answer> = answerRepository.findAllBySurveyResponseId(responseId)

    private fun validateMatchesType(type: QuestionType, input: AnswerReq, validOptionIds: Set<Long>) {
        val nonNull = listOfNotNull(input.textValue, input.questionOptionId, input.scaleValue).size
        if (nonNull != 1)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "exactly one of text_value/question_option_id/scale_value required")
        val ok = when (type) {
            QuestionType.TEXT -> !input.textValue.isNullOrBlank()
            QuestionType.SINGLE_CHOICE -> input.questionOptionId != null && input.questionOptionId in validOptionIds
            QuestionType.SCALE -> input.scaleValue in 1..5
        }
        if (!ok) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "answer does not match question type $type")
    }

    private fun newToken(): String {
        val bytes = ByteArray(24).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
