package com.discord.LocalAIDiscordAgent.promptBuilderChains;

import com.discord.LocalAIDiscordAgent.llmClients.chatSummary.model.ChatSummary;
import com.discord.LocalAIDiscordAgent.llmClients.chatSummary.repository.ChatSummaryRepository;
import com.discord.LocalAIDiscordAgent.llmClients.chatSummary.records.SummaryRecords.Fact;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.llmCallChains.LLMCallChain;
import com.discord.LocalAIDiscordAgent.systemMessage.SystemMessageFactory;
import com.discord.LocalAIDiscordAgent.systemMessage.SystemMessagePresets;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class PromptService {

    private final ChatSummaryRepository repo;
    private final SystemMessageFactory systemMessageFactory;
    private final LLMCallChain llmCallChain;

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    public PromptService(
            ChatSummaryRepository repo,
            SystemMessageFactory systemMessageFactory,
            LLMCallChain llmCallChain
    ) {
        this.systemMessageFactory = systemMessageFactory;
        this.repo = repo;
        this.llmCallChain = llmCallChain;
    }

    public String getSystemPromptAsJson(
            DiscGlobalData discGlobalData
    ) {
        RuntimeContext runtimeContext = llmCallChain.executeContextChainRuntime(discGlobalData);
        if (discGlobalData.getImagePath() != null)  {
            return null;
        }
        return systemMessageFactory.buildSystemMessage(
                buildSystemMessageConfig(runtimeContext)
        );
    }

    private SystemMessageConfig buildSystemMessageConfig(RuntimeContext context) {
        SystemMessageConfig baseConfig = SystemMessagePresets.qwenFriendlyDefault();
        return SystemMessagePresets.withMessageMemory(baseConfig, context);
    }

}