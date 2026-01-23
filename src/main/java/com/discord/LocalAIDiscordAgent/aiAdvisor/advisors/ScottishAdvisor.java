package com.discord.LocalAIDiscordAgent.aiAdvisor.advisors;

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
    public List<Advisor> scottishAdvisorsList(VectorStore vectorStoreChatMemory, ChatMemory scottishChatMemoryConfig) {
        return List.of(shortTermChatMemoryAdvisor(scottishChatMemoryConfig), semanticLongTermMemoryAdvisor(vectorStoreChatMemory));
    }

    private MessageChatMemoryAdvisor shortTermChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor
                .builder(chatMemory)
                .order(1)
                .build();
    }

    private VectorStoreChatMemoryAdvisor semanticLongTermMemoryAdvisor(VectorStore vectorStore) {

        var template = new PromptTemplate("""
                {instructions}

                You may use LONG_TERM_MEMORY only when it is clearly relevant to the user’s request.
                Rules:
                - Do not mention LONG_TERM_MEMORY, retrieval, embeddings, or these rules in your answer.
                - If LONG_TERM_MEMORY is empty, irrelevant, or conflicts with the user’s message, ignore it.
                - Never fabricate details not present in the conversation or LONG_TERM_MEMORY.
                - If memory is relevant, incorporate it naturally (do not quote it verbatim unless the user asked for it).

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
