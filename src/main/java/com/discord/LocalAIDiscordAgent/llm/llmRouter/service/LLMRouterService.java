package com.discord.LocalAIDiscordAgent.llm.llmRouter.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmRouter.dto.LLMRouterDTO;
import com.discord.LocalAIDiscordAgent.llm.llmRouter.preparation.LLMRouterPreparation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LLMRouterService {

    private final LLMRouterPreparation llmRouterPreparation;

    public LLMRouterService(LLMRouterPreparation llmRouterPreparation) {
        this.llmRouterPreparation = llmRouterPreparation;
    }

    public LLMRouterDTO callLLMRouter (DiscGlobalData discGlobalData){
        return llmRouterPreparation.prepare(discGlobalData);
    }
}
