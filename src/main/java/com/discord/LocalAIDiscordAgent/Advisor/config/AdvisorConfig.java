package com.discord.LocalAIDiscordAgent.Advisor.config;


import com.discord.LocalAIDiscordAgent.Advisor.advisor.RecentChatMemoryAdvisor;
import com.discord.LocalAIDiscordAgent.Advisor.advisor.WebMemoryAdvisor;
import com.discord.LocalAIDiscordAgent.Advisor.filters.ChunkMergeFilterWebSearchStore;
import com.discord.LocalAIDiscordAgent.Advisor.templates.AdvisorTemplates;
import com.discord.LocalAIDiscordAgent.chatMemory.service.RecentChatMemoryService;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
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
            ChatMemory webMemory,
            RecentChatMemoryService recentChatMemoryService
    ) {
        return List.of(
                recentChatMemoryAdvisor(recentChatMemoryService),
//                longTermMemoryAdvisor(vectorStoreChatMemory),
//                webSearchAdvisor(vectorStoreWebSearchMemory),
                webMemoryAdvisor(webMemory));
    }

    public RecentChatMemoryAdvisor recentChatMemoryAdvisor(RecentChatMemoryService recentChatMemoryService){
        return RecentChatMemoryAdvisor.builder(recentChatMemoryService)
                .order(0)
                .build();
    }

    public VectorStoreChatMemoryAdvisor longTermMemoryAdvisor(VectorStore vectorStoreChatMemory) {
        return VectorStoreChatMemoryAdvisor.builder(vectorStoreChatMemory)
                .order(1)
                .defaultTopK(2)
                .build();
    }

    public QuestionAnswerAdvisor webSearchAdvisor(VectorStore vectorStoreWebSearchMemory) {
        return QuestionAnswerAdvisor.builder(new ChunkMergeFilterWebSearchStore(vectorStoreWebSearchMemory))
                .order(2)
                .searchRequest(SearchRequest.builder()
                        .topK(1)
                        .similarityThreshold(0.70)
                        .filterExpression("tier == 'WEB_SEARCH'")
                        .build())
                .promptTemplate(AdvisorTemplates.WEB_SEARCH_QUESTION_ANSWER)
                .build();
    }


    public WebMemoryAdvisor webMemoryAdvisor (ChatMemory webMemory){
        return WebMemoryAdvisor.builder(webMemory)
                .order(3)
                .build();
    }



}
