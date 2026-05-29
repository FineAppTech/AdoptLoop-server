package com.tnear.adoptloop.survey.draft

import com.tnear.adoptloop.domain.Axis
import com.tnear.adoptloop.domain.QuestionType
import com.tnear.adoptloop.survey.OptionReq
import com.tnear.adoptloop.survey.QuestionReq
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

data class SurveyDraftPayload(val title: String, val questions: List<QuestionReq>)

@Component
class SurveyDraftParser(private val objectMapper: ObjectMapper) {

    fun parse(raw: String): SurveyDraftPayload {
        val json = extractJson(raw)
        val root = objectMapper.readTree(json)
        val title = root["title"]?.asString()?.takeIf(String::isNotBlank) ?: error("missing or blank title")
        val questions = root["questions"]?.mapIndexed { index, node -> toQuestion(node, index + 1) }
            ?: error("missing questions")
        require(questions.isNotEmpty()) { "no questions in LLM output" }
        return SurveyDraftPayload(title, questions)
    }

    private fun extractJson(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        require(start >= 0 && end > start) { "no JSON object in LLM output" }
        return raw.substring(start, end + 1)
    }

    private fun toQuestion(node: JsonNode, order: Int): QuestionReq {
        // asString()은 JSON null(NullNode)에서 ""를 반환하므로, 빈 값은 명시적으로 거른다(→ 503).
        val type = QuestionType.valueOf(node["type"].asString())
        val axis = node["axis"]?.takeIf { !it.isNull }?.asString()?.let(Axis::valueOf)
        val text = node["text"]?.asString()?.takeIf(String::isNotBlank) ?: error("missing or blank question text")
        val options = node["options"]?.mapIndexed { index, option -> OptionReq(option.asString(), index + 1) }
            ?: emptyList()
        return QuestionReq(
            type = type,
            text = text,
            orderIndex = order,
            required = true,
            axis = axis,
            options = options,
        )
    }
}
