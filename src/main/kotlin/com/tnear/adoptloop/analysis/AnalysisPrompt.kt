package com.tnear.adoptloop.analysis

import com.tnear.adoptloop.domain.Adoption
import tools.jackson.databind.ObjectMapper

object AnalysisPrompt {
    fun build(adoption: Adoption, aggregate: AggregateRes, objectMapper: ObjectMapper): String = """
        당신은 사내 도입 정착도 분석가입니다. 다음 도입과 집계를 분석하여 JSON으로만 응답:

        {
          "adoption_score": 0-100,
          "usage_score": 0-100,
          "behavior_score": 0-100,
          "value_score": 0-100,
          "positive_signals": ["..."],
          "resistance_factors": ["..."],
          "risks": ["..."],
          "suggested_action_items": [
            {"title":"...","description":"...","priority":"HIGH|MEDIUM|LOW"}
          ]
        }

        도입: ${adoption.name} / 목표: ${adoption.goal}
        집계:
        ${objectMapper.writeValueAsString(aggregate)}
    """.trimIndent()
}
