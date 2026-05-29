package com.tnear.adoptloop.analysis

import com.tnear.adoptloop.domain.Priority
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

data class ParsedAnalysisDto(
    val scores: ScoresVo,
    val signals: SignalsVo,
    val suggested: List<SuggestedActionItemVo>,
)
data class ScoresVo(val adoption: Int, val usage: Int, val behavior: Int, val value: Int)
data class SignalsVo(val positive: List<String>, val resistance: List<String>, val risks: List<String>)

@Component
class AnalysisParser(private val objectMapper: ObjectMapper) {
    fun parse(raw: String): ParsedAnalysisDto {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        require(start >= 0 && end > start) { "no JSON object in LLM output" }
        val root = objectMapper.readTree(raw.substring(start, end + 1))
        return ParsedAnalysisDto(
            scores = ScoresVo(
                root["adoption_score"].asInt(), root["usage_score"].asInt(),
                root["behavior_score"].asInt(), root["value_score"].asInt(),
            ),
            signals = SignalsVo(
                root["positive_signals"].toStringList(),
                root["resistance_factors"].toStringList(),
                root["risks"].toStringList(),
            ),
            // .values()로 Collection<JsonNode>를 얻어 stdlib map을 쓴다.
            // JsonNode에는 member fun <R> map(Function)이 있어 node.map{}이 그쪽으로 묶이기 때문.
            suggested = root["suggested_action_items"]?.values()?.map { item ->
                SuggestedActionItemVo(
                    title = item["title"]?.asString()?.takeIf(String::isNotBlank)
                        ?: error("missing or blank action item title"),
                    description = item["description"]?.takeIf { !it.isNull }?.asString(),
                    priority = Priority.valueOf(item["priority"].asString()),
                )
            } ?: emptyList(),
        )
    }

    // asString()은 JSON null(NullNode)에서 ""를 반환하므로 toStringList는 null/missing만 빈 리스트로 처리.
    private fun JsonNode?.toStringList(): List<String> =
        this?.values()?.map { it.asString() } ?: emptyList()
}
