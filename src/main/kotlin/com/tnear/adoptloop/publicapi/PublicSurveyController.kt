package com.tnear.adoptloop.publicapi

import com.tnear.adoptloop.domain.Survey
import com.tnear.adoptloop.domain.repository.QuestionOptionRepository
import com.tnear.adoptloop.domain.repository.QuestionRepository
import com.tnear.adoptloop.survey.QuestionVo
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/public")
class PublicSurveyController(
    private val responseService: PublicResponseService,
    private val questionRepository: QuestionRepository,
    private val optionRepository: QuestionOptionRepository,
) {
    @GetMapping("/surveys/{slug}")
    fun get(@PathVariable slug: String): PublicSurveyRes = toView(responseService.loadBySlug(slug))

    @PostMapping("/surveys/{slug}/responses")
    @ResponseStatus(HttpStatus.CREATED)
    fun start(@PathVariable slug: String): ResponseTokenRes =
        ResponseTokenRes(responseService.startResponse(slug).accessToken)

    @GetMapping("/responses/{token}")
    fun load(@PathVariable token: String): PublicResponseRes {
        val (response, survey) = responseService.loadByToken(token)
        val answers = responseService.loadAnswers(response.id!!).map {
            AnswerReq(it.questionId, it.textValue, it.questionOptionId, it.scaleValue)
        }
        return PublicResponseRes(toView(survey), response.status, response.submittedAt, answers)
    }

    @PutMapping("/responses/{token}/answers")
    fun submit(@PathVariable token: String, @RequestBody inputs: List<AnswerReq>): PublicResponseRes {
        responseService.submit(token, inputs)
        return load(token)
    }

    private fun toView(survey: Survey): PublicSurveyRes {
        val questions = questionRepository.findAllBySurveyIdOrderByOrderIndex(survey.id!!)
        val optionsByQuestion = if (questions.isEmpty()) emptyMap()
            else optionRepository.findAllByQuestionIdInOrderByOrderIndex(questions.mapNotNull { it.id })
                .groupBy { it.questionId }
        val questionViews = questions.map { QuestionVo.from(it, optionsByQuestion[it.id] ?: emptyList()) }
        return PublicSurveyRes(survey.title, survey.deadline, questionViews)
    }
}
