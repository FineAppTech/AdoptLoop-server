package com.tnear.adoptloop.survey

import com.tnear.adoptloop.domain.Axis
import com.tnear.adoptloop.domain.Question
import com.tnear.adoptloop.domain.QuestionOption
import com.tnear.adoptloop.domain.QuestionType
import com.tnear.adoptloop.domain.Survey
import com.tnear.adoptloop.domain.SurveyStatus
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class QuestionReq(
    val type: QuestionType,
    @field:NotBlank val text: String,
    val orderIndex: Int,
    val required: Boolean = true,
    val axis: Axis? = null,
    val options: List<OptionReq> = emptyList(),
)

data class OptionReq(
    @field:NotBlank val text: String,
    val orderIndex: Int,
)

data class SurveyDraftReq(
    @field:Future val deadline: Instant,
)

data class OptionVo(val id: Long, val text: String, val orderIndex: Int) {
    companion object {
        fun from(option: QuestionOption) = OptionVo(option.id!!, option.text, option.orderIndex)
    }
}

data class QuestionVo(
    val id: Long,
    val type: QuestionType,
    val text: String,
    val orderIndex: Int,
    val required: Boolean,
    val axis: Axis?,
    val options: List<OptionVo>,
) {
    companion object {
        fun from(question: Question, options: List<QuestionOption>) = QuestionVo(
            question.id!!, question.type, question.text, question.orderIndex,
            question.required, question.axis, options.map(OptionVo::from),
        )
    }
}

data class SurveyRes(
    val id: Long,
    val adoptionId: Long,
    val title: String,
    val publicSlug: String,
    val status: SurveyStatus,
    val deadline: Instant,
    val publishedAt: Instant?,
    val createdAt: Instant,
) {
    companion object {
        fun from(survey: Survey) = SurveyRes(
            survey.id!!, survey.adoptionId, survey.title, survey.publicSlug,
            survey.status, survey.deadline, survey.publishedAt, survey.createdAt,
        )
    }
}

data class SurveyDetailRes(val survey: SurveyRes, val questions: List<QuestionVo>)
