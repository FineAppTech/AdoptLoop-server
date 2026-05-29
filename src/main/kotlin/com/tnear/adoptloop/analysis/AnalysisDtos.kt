package com.tnear.adoptloop.analysis

import com.tnear.adoptloop.domain.Priority
import java.time.Instant

data class AnalysisRes(
    val id: Long,
    val surveyId: Long,
    val adoptionScore: Int,
    val usageScore: Int,
    val behaviorScore: Int,
    val valueScore: Int,
    val positiveSignals: List<String>,
    val resistanceFactors: List<String>,
    val risks: List<String>,
    val rawOutput: String,
    val createdAt: Instant,
)

data class SuggestedActionItemVo(
    val title: String,
    val description: String? = null,
    val priority: Priority,
)

data class AnalysisRunRes(
    val analysis: AnalysisRes,
    val suggestedActionItems: List<SuggestedActionItemVo>,
)
