package com.tnear.adoptloop.survey.draft

import com.tnear.adoptloop.domain.Adoption

object SurveyDraftPrompt {
    fun build(adoption: Adoption): String = """
        당신은 사내 도입 정착도 설문 설계 전문가입니다. 아래 도입 정보를 바탕으로
        한국어 설문 문항을 설계하세요. 각 문항은 type(TEXT/SINGLE_CHOICE/SCALE)을 가지며,
        SCALE 문항은 axis(USAGE/BEHAVIOR/VALUE)를 명시합니다. 모든 enum 값은 UPPERCASE.

        **문항 구성 (반드시 준수):**
        - SCALE 4-6개 — USAGE/BEHAVIOR/VALUE 각 축당 **최소 1개** 포함
        - SINGLE_CHOICE 2-3개
        - TEXT 1-2개 (주관식, 자유 의견)
        - 합계 7-10개

        JSON으로만 응답:

        {
          "title": "...",
          "questions": [
            { "type": "SCALE", "text": "...", "axis": "USAGE" },
            { "type": "SINGLE_CHOICE", "text": "...", "options": ["A","B","C"] },
            { "type": "TEXT", "text": "..." }
          ]
        }

        도입 정보
        - 이름: ${adoption.name}
        - 목표: ${adoption.goal}
        - 대상자: ${adoption.targetAudience}
        - 우려사항: ${adoption.concern ?: "없음"}
    """.trimIndent()
}
