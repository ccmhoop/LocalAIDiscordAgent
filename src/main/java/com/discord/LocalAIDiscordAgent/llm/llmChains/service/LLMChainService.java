package com.discord.LocalAIDiscordAgent.llm.llmChains.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llm.llmChains.llmCallChains.LLMCallChain;
import com.discord.LocalAIDiscordAgent.llm.systemMessage.SystemMessageFactory;
import com.discord.LocalAIDiscordAgent.llm.systemMessage.SystemMessagePresets;
import com.discord.LocalAIDiscordAgent.llm.systemMessage.records.SystemMsgRecords.RuntimeContext;
import com.discord.LocalAIDiscordAgent.llm.systemMessage.records.SystemMsgRecords.SystemMessageConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LLMChainService {

    private final SystemMessageFactory systemMessageFactory;
    private final LLMCallChain llmCallChain;

    public LLMChainService(
            SystemMessageFactory systemMessageFactory,
            LLMCallChain llmCallChain
    ) {
        this.systemMessageFactory = systemMessageFactory;
        this.llmCallChain = llmCallChain;
    }

    public String getSystemPromptAsJson(DiscGlobalData discGlobalData, boolean requiresContext) {
        RuntimeContext runtimeContext = llmCallChain.executeTextContextRuntime(discGlobalData, requiresContext);

        return systemMessageFactory.buildSystemMessage(
                buildSystemMessageConfig(runtimeContext)
        );
    }

    private SystemMessageConfig buildSystemMessageConfig(RuntimeContext context) {
        SystemMessageConfig baseConfig = SystemMessagePresets.qwenFriendlyDefault();
        return SystemMessagePresets.withMessageMemory(baseConfig, context);
    }
}