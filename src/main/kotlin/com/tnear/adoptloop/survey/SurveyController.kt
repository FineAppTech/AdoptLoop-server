package com.tnear.adoptloop.survey

import com.tnear.adoptloop.admin.auth.AdminContext
import jakarta.validation.Valid
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
@RequestMapping("/api/admin")
class SurveyController(
    private val service: SurveyService,
    private val adminContext: AdminContext,
) {
    @GetMapping("/surveys/{id}")
    fun detail(@PathVariable id: Long): SurveyDetailRes = toDetail(id)

    @PutMapping("/surveys/{id}/questions")
    fun replaceQuestions(@PathVariable id: Long, @Valid @RequestBody inputs: List<QuestionReq>): SurveyDetailRes {
        service.replaceQuestions(adminContext.require(), id, inputs)
        return toDetail(id)
    }

    @PostMapping("/surveys/{id}/publish")
    @ResponseStatus(HttpStatus.OK)
    fun publish(@PathVariable id: Long): SurveyRes =
        SurveyRes.from(service.publish(adminContext.require(), id))

    private fun toDetail(id: Long): SurveyDetailRes {
        val (survey, questions) = service.detail(adminContext.require(), id)
        val optionsByQuestion = service.loadOptions(questions.mapNotNull { it.id })
        return SurveyDetailRes(
            survey = SurveyRes.from(survey),
            questions = questions.map { QuestionVo.from(it, optionsByQuestion[it.id] ?: emptyList()) },
        )
    }
}
