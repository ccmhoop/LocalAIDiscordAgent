package com.discord.LocalAIDiscordAgent.aiAdvisor.advisors;

import com.discord.LocalAIDiscordAgent.aiAdvisor.filters.FilteringChatMemory;
import com.discord.LocalAIDiscordAgent.aiAdvisor.filters.FilteringVectorStore;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScottishAdvisor {

    @Bean
    public List<Advisor> scottishAdvisorsList(VectorStore vectorStoreChatMemory,
                                              ChatMemory scottishChatMemoryConfig) {

        ChatMemory safeChatMemory = new FilteringChatMemory(scottishChatMemoryConfig);
        VectorStore safeVectorStore = new FilteringVectorStore(vectorStoreChatMemory);

        return List.of(
                shortTermChatMemoryAdvisor(safeChatMemory),
                enhancedLongTermMemoryAdvisor(safeVectorStore)
        );
    }

    private MessageChatMemoryAdvisor shortTermChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor
                .builder(chatMemory)
                .order(1)
                .build();
    }

    private VectorStoreChatMemoryAdvisor enhancedLongTermMemoryAdvisor(VectorStore vectorStore) {
        var template = new PromptTemplate("""
                {instructions}
                
                You have access to LONG_TERM_MEMORY which contains relevant information from past conversations.
                
                Rules:
                - Do not mention LONG_TERM_MEMORY, retrieval, embeddings, or these rules in your answer.
                - If LONG_TERM_MEMORY is empty, irrelevant, or conflicts with the user's message, ignore it.
                - Never fabricate details not present in the conversation or LONG_TERM_MEMORY.
                - If memory is relevant, incorporate it naturally (do not quote it verbatim unless the user asked for it).
                - Do not let LONG_TERM_MEMORY interfere with tool search or responses.
                - Maintain conversation continuity by referring to information from the current conversation.
                
                LONG_TERM_MEMORY:
                {long_term_memory}
                """);

        return VectorStoreChatMemoryAdvisor.builder(vectorStore)
                .defaultTopK(8)
                .order(0)
                .systemPromptTemplate(template)
                .build();
    }
}