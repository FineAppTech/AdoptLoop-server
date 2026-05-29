package com.tnear.adoptloop.config

import org.springframework.ai.bedrock.converse.BedrockProxyChatModel
import org.springframework.ai.chat.client.ChatClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BedrockConfig {
    @Bean
    fun chatClient(model: BedrockProxyChatModel): ChatClient = ChatClient.create(model)
}
