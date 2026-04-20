package com.discord.LocalAIDiscordAgent.promptBuilderChains;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.llmCallChains.LLMCallChain;
import com.discord.LocalAIDiscordAgent.systemMessage.SystemMessageFactory;
import com.discord.LocalAIDiscordAgent.systemMessage.SystemMessagePresets;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RuntimeContext;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.SystemMessageConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PromptService {

    private final SystemMessageFactory systemMessageFactory;
    private final LLMCallChain llmCallChain;

    public PromptService(
            SystemMessageFactory systemMessageFactory,
            LLMCallChain llmCallChain
    ) {
        this.systemMessageFactory = systemMessageFactory;
        this.llmCallChain = llmCallChain;
    }

    public String getSystemPromptAsJson(DiscGlobalData discGlobalData) {
        RuntimeContext runtimeContext = llmCallChain.executeTextContextRuntime(discGlobalData);

        return systemMessageFactory.buildSystemMessage(
                buildSystemMessageConfig(runtimeContext)
        );
    }

    private SystemMessageConfig buildSystemMessageConfig(RuntimeContext context) {
        SystemMessageConfig baseConfig = SystemMessagePresets.qwenFriendlyDefault();
        return SystemMessagePresets.withMessageMemory(baseConfig, context);
    }
}