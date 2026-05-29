package com.tnear.adoptloop.publicapi

import com.tnear.adoptloop.domain.ResponseStatus
import com.tnear.adoptloop.survey.QuestionVo
import java.time.Instant

data class PublicSurveyRes(val title: String, val deadline: Instant, val questions: List<QuestionVo>)

data class ResponseTokenRes(val accessToken: String)

data class AnswerReq(
    val questionId: Long,
    val textValue: String? = null,
    val questionOptionId: Long? = null,
    val scaleValue: Int? = null,
)

data class PublicResponseRes(
    val survey: PublicSurveyRes,
    val status: ResponseStatus,
    val submittedAt: Instant?,
    val answers: List<AnswerReq>,
)
