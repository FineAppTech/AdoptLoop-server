package com.tnear.adoptloop.survey

import com.tnear.adoptloop.domain.Adoption
import com.tnear.adoptloop.domain.Question
import com.tnear.adoptloop.domain.QuestionOption
import com.tnear.adoptloop.domain.Survey
import com.tnear.adoptloop.domain.SurveyStatus
import com.tnear.adoptloop.domain.repository.AdoptionRepository
import com.tnear.adoptloop.domain.repository.QuestionOptionRepository
import com.tnear.adoptloop.domain.repository.QuestionRepository
import com.tnear.adoptloop.domain.repository.SurveyRepository
import com.tnear.adoptloop.survey.publish.SurveyPublisher
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

@Service
@Transactional
class SurveyService(
    private val surveyRepository: SurveyRepository,
    private val adoptionRepository: AdoptionRepository,
    private val questionRepository: QuestionRepository,
    private val optionRepository: QuestionOptionRepository,
    private val publisher: SurveyPublisher,
) {
    private val random = SecureRandom()

    fun createDraft(adminId: Long, adoptionId: Long, title: String, deadline: Instant): Survey {
        requireAdoptionOwned(adminId, adoptionId)
        if (!deadline.isAfter(Instant.now()))
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "deadline must be in future")
        return surveyRepository.save(Survey(
            adoptionId = adoptionId,
            title = title,
            publicSlug = newSlug(),
            deadline = deadline,
        ))
    }

    fun replaceQuestions(adminId: Long, surveyId: Long, inputs: List<QuestionReq>): Survey {
        val survey = requireSurveyOwned(adminId, surveyId)
        if (survey.status != SurveyStatus.DRAFT)
            throw ResponseStatusException(HttpStatus.CONFLICT, "survey already published")
        val existingIds = questionRepository.findAllBySurveyIdOrderByOrderIndex(surveyId).mapNotNull { it.id }
        if (existingIds.isNotEmpty()) optionRepository.deleteAllByQuestionIdIn(existingIds)
        questionRepository.deleteAllBySurveyId(surveyId)
        questionRepository.flush()
        inputs.forEach { input ->
            val question = questionRepository.save(Question(
                surveyId = surveyId,
                type = input.type, text = input.text, orderIndex = input.orderIndex,
                required = input.required, axis = input.axis,
            ))
            input.options.forEach { option ->
                optionRepository.save(QuestionOption(
                    questionId = question.id!!, text = option.text, orderIndex = option.orderIndex,
                ))
            }
        }
        return survey
    }

    fun publish(adminId: Long, surveyId: Long): Survey {
        val survey = requireSurveyOwned(adminId, surveyId)
        if (survey.status != SurveyStatus.DRAFT)
            throw ResponseStatusException(HttpStatus.CONFLICT, "already published")
        if (!survey.deadline.isAfter(Instant.now()))
            throw ResponseStatusException(HttpStatus.CONFLICT, "deadline already passed")
        val questions = questionRepository.findAllBySurveyIdOrderByOrderIndex(surveyId)
        if (questions.isEmpty())
            throw ResponseStatusException(HttpStatus.CONFLICT, "questions required to publish")
        survey.status = SurveyStatus.PUBLISHED
        survey.publishedAt = Instant.now()
        publisher.scheduleAnnouncement(survey)
        return survey
    }

    fun createDraftWithQuestions(
        adminId: Long,
        adoptionId: Long,
        title: String,
        deadline: Instant,
        questions: List<QuestionReq>,
    ): Survey {
        val survey = createDraft(adminId, adoptionId, title, deadline)
        replaceQuestions(adminId, survey.id!!, questions)
        return survey
    }

    @Transactional(readOnly = true)
    fun detail(adminId: Long, surveyId: Long): Pair<Survey, List<Question>> {
        val survey = requireSurveyOwned(adminId, surveyId)
        val questions = questionRepository.findAllBySurveyIdOrderByOrderIndex(surveyId)
        return survey to questions
    }

    @Transactional(readOnly = true)
    fun loadOptions(questionIds: Collection<Long>): Map<Long, List<QuestionOption>> =
        if (questionIds.isEmpty()) emptyMap()
        else optionRepository.findAllByQuestionIdInOrderByOrderIndex(questionIds).groupBy { it.questionId }

    private fun requireAdoptionOwned(adminId: Long, adoptionId: Long): Adoption {
        val adoption = adoptionRepository.findById(adoptionId)
            .orElseThrow { NoSuchElementException("adoption $adoptionId") }
        if (adoption.adminId != adminId) throw ResponseStatusException(HttpStatus.FORBIDDEN, "not owner")
        return adoption
    }

    private fun requireSurveyOwned(adminId: Long, surveyId: Long): Survey {
        val survey = surveyRepository.findById(surveyId)
            .orElseThrow { NoSuchElementException("survey $surveyId") }
        requireAdoptionOwned(adminId, survey.adoptionId)
        return survey
    }

    private fun newSlug(): String {
        val bytes = ByteArray(12).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
