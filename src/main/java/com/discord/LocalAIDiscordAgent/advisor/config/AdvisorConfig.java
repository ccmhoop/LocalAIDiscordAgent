package com.discord.LocalAIDiscordAgent.advisor.config;


import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.advisor.RecentChatMemoryAdvisor;
import com.discord.LocalAIDiscordAgent.chatMemory.toolChatMemory.advisor.WebMemoryAdvisor;
import com.discord.LocalAIDiscordAgent.webSearch.advisor.WebQuestionAnswerAdvisor;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.service.RecentChatMemoryService;
import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.advisor.GroupChatMemoryAdvisor;
import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.service.GroupChatMemoryService;
import com.discord.LocalAIDiscordAgent.webSearch.service.WebSearchMemoryService;
import org.springframework.ai.chat.client.advisor.api.Advisor;
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
            RecentChatMemoryService recentChatMemoryService,
            GroupChatMemoryService groupChatMemoryAdvisor,
            WebSearchMemoryService webSearchMemoryService
    ) {
        return List.of(
                recentChatMemoryAdvisor(recentChatMemoryService),
                groupChatMemoryAdvisor(groupChatMemoryAdvisor),
//                longTermMemoryAdvisor(vectorStoreChatMemory),
                webSearchAdvisor(vectorStoreWebSearchMemory, webSearchMemoryService),
                webMemoryAdvisor(webMemory)
        );
    }

    public RecentChatMemoryAdvisor recentChatMemoryAdvisor(RecentChatMemoryService recentChatMemoryService){
        return RecentChatMemoryAdvisor.builder(recentChatMemoryService)
                .order(0)
                .build();
    }

    public GroupChatMemoryAdvisor groupChatMemoryAdvisor (GroupChatMemoryService service){
        return GroupChatMemoryAdvisor.builder(service)
                .order(1)
                .build();
    }

    public VectorStoreChatMemoryAdvisor longTermMemoryAdvisor(VectorStore vectorStoreChatMemory) {
        return VectorStoreChatMemoryAdvisor.builder(vectorStoreChatMemory)
                .order(1)
                .defaultTopK(2)
                .build();
    }

    public WebQuestionAnswerAdvisor webSearchAdvisor(VectorStore vectorStoreWebSearchMemory, WebSearchMemoryService webSearchMemoryService) {
        return WebQuestionAnswerAdvisor.builder( webSearchMemoryService, vectorStoreWebSearchMemory)
                .order(2)
                .searchRequest(SearchRequest.builder()
                        .topK(2)
                        .similarityThreshold(0.70)
                        .filterExpression("tier == 'WEB_SEARCH'")
                        .build())
                .build();
    }


    public WebMemoryAdvisor webMemoryAdvisor (ChatMemory webMemory){
        return WebMemoryAdvisor.builder(webMemory)
                .order(3)
                .build();
    }



}
