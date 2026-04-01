package com.discord.LocalAIDiscordAgent.promptBuilderChains;

import com.discord.LocalAIDiscordAgent.llmMain.chatSummary.model.ChatSummary;
import com.discord.LocalAIDiscordAgent.llmMain.chatSummary.repository.ChatSummaryRepository;
import com.discord.LocalAIDiscordAgent.llmMain.chatSummary.records.SummaryRecords.Fact;
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
    private final DiscGlobalData discGlobalData;
    private final SystemMessageFactory systemMessageFactory;
    private final LLMCallChain llmCallChain;

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    public PromptService(
            ChatSummaryRepository repo,
            DiscGlobalData discGlobalData,
            SystemMessageFactory systemMessageFactory,
            LLMCallChain llmCallChain
    ) {
        this.systemMessageFactory = systemMessageFactory;
        this.discGlobalData = discGlobalData;
        this.repo = repo;
        this.llmCallChain = llmCallChain;
    }


    public String getSystemPromptAsJson() {
        RuntimeContext runtimeContext = llmCallChain.executeContextChainRuntime();
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

    private Memory buildMemory() {
        String id;
        if (discGlobalData.getGroupChatMemory() == null) {
            id = discGlobalData.getGroupConversationId();
        }else if (discGlobalData.getRecentMessages() != null) {
            id = discGlobalData.getConversationId();
        }else{
            return null;
        }

        try {
            Optional<ChatSummary> mem = repo.findById(id);
            if (mem.isEmpty()) {
                return null;
            }

            String summaryText = safe(mem.get().getSummary());
            String factsJson = mem.get().getFactsJson();
            List<FactsMemory> factsMemory = getFactsMemoryList(factsJson);

            return new Memory(summaryText, factsMemory);
        } catch (Exception e) {
            log.error("Failed to build chat summary: {}", e.getMessage(), e);
            return null;
        }
    }

    private List<FactsMemory> getFactsMemoryList(String factsJson) throws Exception {
        List<Fact> facts = (factsJson == null || factsJson.isBlank() || factsJson.equals("[]"))
                ? List.of()
                : getFacts(factsJson);

        List<FactsMemory> factsMemory = new ArrayList<>(facts.size());

        for (Fact fact : facts) {
            if (fact == null) {
                continue;
            }
            factsMemory.add(new FactsMemory(
                    fact.key(),
                    fact.value(),
                    fact.confidence()
            ));
        }
        return factsMemory;
    }

    private List<Fact> getFacts(String factsJson) throws Exception {
        return objectMapper.readValue(factsJson, new TypeReference<List<Fact>>() {
        });
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

}