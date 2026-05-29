package com.tnear.adoptloop.analysis

data class AggregateRes(
    val participants: Int,
    val targetCount: Int,
    val participationRate: Double,
    val perQuestion: List<QuestionAggregateVo>,
)

sealed interface QuestionAggregateVo { val questionId: Long; val type: String }

data class ChoiceAggregateVo(
    override val questionId: Long,
    val distribution: List<DistributionBucketVo>,
) : QuestionAggregateVo { override val type = "SINGLE_CHOICE" }

data class ScaleAggregateVo(
    override val questionId: Long,
    val axis: String?,
    val average: Double,
    val count: Long,
) : QuestionAggregateVo { override val type = "SCALE" }

data class TextAggregateVo(
    override val questionId: Long,
    val values: List<String>,
) : QuestionAggregateVo { override val type = "TEXT" }

data class DistributionBucketVo(val optionId: Long, val count: Long)
