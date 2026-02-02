package com.discord.LocalAIDiscordAgent.aiAdvisor.advisors;

import com.discord.LocalAIDiscordAgent.aiAdvisor.filters.FilteringChatMemory;
import com.discord.LocalAIDiscordAgent.aiAdvisor.filters.FilteringVectorStore;
import com.discord.LocalAIDiscordAgent.aiAdvisor.filters.ChunkMergeFilterWebSearchStore;
import com.discord.LocalAIDiscordAgent.aiAdvisor.templates.AdvisorTemplates;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class ScottishAdvisor {

    public static List<Advisor> scottishAdvisorsList(
            VectorStore vectorStoreChatMemory,
            ChatMemory scottishChatMemoryConfig,
            VectorStore vectorStoreWebSearchMemory
    ) {

        VectorStore mergedWebStore = new ChunkMergeFilterWebSearchStore(vectorStoreWebSearchMemory);

        return List.of(
                shortTermChatMemoryAdvisor(scottishChatMemoryConfig),
                enhancedLongTermMemoryAdvisor(vectorStoreChatMemory),
                webSearchQuestionAnswerAdvisor(mergedWebStore)
        );
    }

    public static List<Advisor> toolAdvisor(
            ChatMemory scottishChatMemoryConfig,
            VectorStore vectorStoreWebSearchMemory
    ) {
        VectorStore mergedWebStore = new ChunkMergeFilterWebSearchStore(vectorStoreWebSearchMemory);

        return List.of(
//                webSearchQuestionAnswerAdvisor(mergedWebStore),
                shortTermChatMemoryAdvisor(scottishChatMemoryConfig)
        );
    }

    private static MessageChatMemoryAdvisor shortTermChatMemoryAdvisor(
            ChatMemory chatMemory
    ) {
        return MessageChatMemoryAdvisor.builder(chatMemory)
                .order(0)
                .build();
    }

    private static VectorStoreChatMemoryAdvisor enhancedLongTermMemoryAdvisor(
            VectorStore vectorStore
    ) {
        return VectorStoreChatMemoryAdvisor.builder(vectorStore)
                .defaultTopK(3)
                .systemPromptTemplate(AdvisorTemplates.LONG_TERM_MEMORY)
                .order(1)
                .build();
    }

    private static QuestionAnswerAdvisor webSearchQuestionAnswerAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(8)
                        .similarityThreshold(0.45)
                        .filterExpression("tier == 'WEB_SEARCH'")
                        .build())
                .promptTemplate(AdvisorTemplates.WEB_SEARCH_QUESTION_ANSWER)
                .order(2)
                .build();
    }
}
