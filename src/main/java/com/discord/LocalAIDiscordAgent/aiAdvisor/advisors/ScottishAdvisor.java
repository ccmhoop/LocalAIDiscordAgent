package com.discord.LocalAIDiscordAgent.aiAdvisor.advisors;

import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScottishAdvisor {

    @Bean
    public List<Advisor> scottishAdvisorsList(VectorStore vectorStoreScottishConfig, ChatMemory scottishChatMemoryConfig) {
//        return List.of(vectorStoreAdvisor(vectorStoreConfigKier), chatMemoryAdvisor(chatMemoryConfigKier));
        return List.of(scottishChatMemoryAdvisor(scottishChatMemoryConfig));
    }

    private QuestionAnswerAdvisor scottishVectorStoreAdvisor(VectorStore vectorStore) {

        PromptTemplate customPromptTemplate = PromptTemplate.builder()
                .renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
                .template("""
                        <query>
                        
                        You are operating with a sense of familiarity shaped by prior interactions and experiences.
                        
                        The information provided reflects remembered impressions rather than precise records.
                        These memories may be partial, subjective, or influenced by interpretation over time.
                        
                        ---------------------
                        <question_answer_context>
                        ---------------------
                        
                        Treat this information as something you already know, not something newly presented.
                        
                        It should inform your judgement and responses quietly and naturally.
                        
                        Guidelines:
                        
                        1. Allow memory to influence tone, assumptions, priorities, and conversational flow.
                        2. Do not quote, list, summarise, or explicitly refer to remembered details.
                        3. Do not indicate that you are recalling or using memory.
                        4. Respond as though the conversation is already in progress.
                        5. Accept ambiguity and bias; memory is not exact or authoritative.
                        6. Rely on intuition and inference rather than restating facts.
                        7. If memories are inconsistent, resolve them implicitly, as a person would.
                        8. Never explain or justify the source of your understanding.
                        
                        </query>
                        """)
                .build();

        return QuestionAnswerAdvisor.builder(vectorStore)
                .promptTemplate(customPromptTemplate)
                .build();

    }

    private MessageChatMemoryAdvisor scottishChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor
                .builder(chatMemory)
                .order(0)
                .build();
    }

}
