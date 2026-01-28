package com.discord.LocalAIDiscordAgent.aiAdvisor.advisors;

import com.discord.LocalAIDiscordAgent.aiAdvisor.filters.FilteringChatMemory;
import com.discord.LocalAIDiscordAgent.aiAdvisor.filters.FilteringVectorStore;
import com.discord.LocalAIDiscordAgent.aiAdvisor.filters.MergingWebVectorStore;
import com.discord.LocalAIDiscordAgent.aiAdvisor.templates.AdvisorTemplates;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScottishAdvisor {

    @Bean
    public List<Advisor> scottishAdvisorsList(VectorStore vectorStoreChatMemory,
                                              ChatMemory scottishChatMemoryConfig,
                                              VectorStore vectorStoreWebSearchMemory) {

        ChatMemory safeChatMemory = new FilteringChatMemory(scottishChatMemoryConfig);
        VectorStore safeVectorStore = new FilteringVectorStore(vectorStoreChatMemory);
        VectorStore mergedWebStore = new MergingWebVectorStore(vectorStoreWebSearchMemory);

        return List.of(
                shortTermChatMemoryAdvisor(safeChatMemory),
                enhancedLongTermMemoryAdvisor(safeVectorStore),
                webSearchQuestionAnswerAdvisor(mergedWebStore)

        );
    }

    public MessageChatMemoryAdvisor shortTermChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory)
                .order(0)
                .build();
    }


    private VectorStoreChatMemoryAdvisor enhancedLongTermMemoryAdvisor(VectorStore vectorStore) {
        return VectorStoreChatMemoryAdvisor.builder(vectorStore)
                .defaultTopK(3)
                .order(1)
                .systemPromptTemplate(AdvisorTemplates.LONG_TERM_MEMORY)
                .build();
    }


    private QuestionAnswerAdvisor webSearchQuestionAnswerAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.50)
                        .topK(3)
                        .build())
                .promptTemplate(AdvisorTemplates.WEB_SEARCH_QUESTION_ANSWER)
                .order(2)
                .build();
    }
}