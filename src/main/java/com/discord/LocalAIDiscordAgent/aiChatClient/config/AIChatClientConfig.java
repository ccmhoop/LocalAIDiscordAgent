package com.discord.LocalAIDiscordAgent.aiChatClient.config;

import com.discord.LocalAIDiscordAgent.aiTools.websearch.AISearchEngineTool;
import com.discord.LocalAIDiscordAgent.aiTools.websearch.AIWebFilterTool;
import com.discord.LocalAIDiscordAgent.aiTools.websearch.AIWebSearchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class AIChatClientConfig {

//    @Bean
//    public ChatClient chatClientOllamaScottish(OllamaChatModel ollamaQwenModelConfig, List<Advisor> scottishAdvisorsList, AIWebFilterTool aiWebFilterTool, AIWebSearchTool aiWebSearchTool) {
//        return ChatClient.builder(ollamaQwenModelConfig)
//                .defaultAdvisors(scottishAdvisorsList)
//                .defaultTools(aiWebSearchTool, aiWebFilterTool)
//                .build();
//    }

    @Bean
    public ChatClient chatClientOllamaScottish(OllamaChatModel ollamaQwenModelConfig, List<Advisor> scottishAdvisorsList,
                                               @Qualifier("webSearch") AIWebSearchTool webSearch,
                                               @Qualifier("webFilterText") AIWebFilterTool webFilterText,
                                               @Qualifier("webSearchEngine") AISearchEngineTool webSearchEngine
                                               ) {

        return ChatClient.builder(ollamaQwenModelConfig)
                .defaultAdvisors(scottishAdvisorsList)
                .defaultTools(webSearch, webSearchEngine, webFilterText)
                .build();
    }


}
