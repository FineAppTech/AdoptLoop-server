package com.tnear.adoptloop.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("adoptloop.slack")
data class SlackProperties(val webhookUrl: String? = null)
