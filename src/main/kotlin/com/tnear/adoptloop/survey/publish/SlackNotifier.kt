package com.tnear.adoptloop.survey.publish

import com.tnear.adoptloop.config.SlackProperties
import com.tnear.adoptloop.domain.Survey
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class SlackNotifier(
    private val slackProperties: SlackProperties,
    @Value("\${adoptloop.public-base-url}") private val publicBaseUrl: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.create()

    fun publishAnnouncement(survey: Survey) {
        val webhookUrl = slackProperties.webhookUrl?.takeIf { it.isNotBlank() } ?: return
        val body = mapOf("text" to """
            *${survey.title}* 설문이 발행되었습니다.
            마감: ${survey.deadline}
            $publicBaseUrl/s/${survey.publicSlug}
        """.trimIndent())
        try {
            restClient.post().uri(webhookUrl).body(body).retrieve().toBodilessEntity()
        } catch (e: Exception) {
            log.warn("Slack notify failed for survey {}: {}", survey.id, e.message)
        }
    }
}
