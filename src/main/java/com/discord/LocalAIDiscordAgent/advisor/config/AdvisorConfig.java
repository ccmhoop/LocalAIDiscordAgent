package com.discord.LocalAIDiscordAgent.advisor.config;


import com.discord.LocalAIDiscordAgent.chatMemory.webChatMemory.advisor.WebChatMemoryAdvisor;
import com.discord.LocalAIDiscordAgent.chatMemory.webChatMemory.service.WebChatMemoryService;
import com.discord.LocalAIDiscordAgent.webSearch.advisor.WebQuestionAnswerAdvisor;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.service.RecentChatMemoryService;
import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.service.GroupChatMemoryService;
import com.discord.LocalAIDiscordAgent.webSearch.service.WebSearchMemoryService;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import java.util.List;

@Configuration
/*
  Under construction.
  @Todo overhaul advisor config to be more flexible and modular and design custom advisors.
*/
public class AdvisorConfig {

    @Bean
    public List<Advisor> agentChatAdvisors(
//            VectorStore vectorStoreChatMemory,
            VectorStore vectorStoreWebSearchMemory,
            WebChatMemoryService webChatMemoryService,
            RecentChatMemoryService recentChatMemoryService,
            GroupChatMemoryService groupChatMemoryAdvisor,
            WebSearchMemoryService webSearchMemoryService
    ) {
        return List.of(
//                longTermMemoryAdvisor(vectorStoreChatMemory),
                webSearchAdvisor(vectorStoreWebSearchMemory, webSearchMemoryService)
//                webMemoryAdvisor(webChatMemoryService)
        );
    }


    private WebChatMemoryAdvisor webMemoryAdvisor (WebChatMemoryService service){
        return WebChatMemoryAdvisor.builder(service)
                .order(2)
                .build();
    }

    private WebQuestionAnswerAdvisor webSearchAdvisor(VectorStore vectorStoreWebSearchMemory, WebSearchMemoryService webSearchMemoryService) {
        return WebQuestionAnswerAdvisor.builder( webSearchMemoryService, vectorStoreWebSearchMemory)
                .order(3)
                .searchRequest(SearchRequest.builder()
                        .topK(2)
                        .similarityThreshold(0.70)
                        .filterExpression("tier == 'WEB_SEARCH'")
                        .build())
                .build();
    }

    private VectorStoreChatMemoryAdvisor longTermMemoryAdvisor(VectorStore vectorStoreChatMemory) {
        return VectorStoreChatMemoryAdvisor.builder(vectorStoreChatMemory)
                .order(1)
                .defaultTopK(2)
                .build();
    }





}
