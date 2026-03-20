package com.discord.LocalAIDiscordAgent.promptBuilderChains;

import com.discord.LocalAIDiscordAgent.chatSummary.model.ChatSummary;
import com.discord.LocalAIDiscordAgent.chatSummary.repository.ChatSummaryRepository;
import com.discord.LocalAIDiscordAgent.chatSummary.records.SummaryRecords.Fact;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.memoryChains.PromptMemoryChain;
import com.discord.LocalAIDiscordAgent.systemMessage.SystemMessageFactory;
import com.discord.LocalAIDiscordAgent.systemMessage.SystemMessagePresets;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.*;
import com.discord.LocalAIDiscordAgent.toolClient.service.ToolQueryGenerationService;
import com.discord.LocalAIDiscordAgent.toolClient.service.ToolRelevantMemoryService;
import com.discord.LocalAIDiscordAgent.toolClient.service.ToolService;
import com.discord.LocalAIDiscordAgent.vectorMemory.webQAMemory.WebQAService;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class PromptService {

    private final ToolService toolService;
    private final ChatSummaryRepository repo;
    private final DiscGlobalData discGlobalData;
    private final SystemMessageFactory systemMessageFactory;
    private final PromptMemoryChain promptMemoryChain;

    List<MergedWebQAItem> webQAResults;
    private Memory baseMemory;


    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    public PromptService(
            ToolService toolService,
            ChatSummaryRepository repo,
            DiscGlobalData discGlobalData,
            SystemMessageFactory systemMessageFactory,
            PromptMemoryChain promptMemoryChain
    ) {
        this.systemMessageFactory = systemMessageFactory;
        this.discGlobalData = discGlobalData;
        this.toolService = toolService;
        this.repo = repo;
        this.promptMemoryChain = promptMemoryChain;
    }


    public String getSystemPromptAsJson() {

        promptMemoryChain.executeMemoryChain();
        return systemMessageFactory.buildSystemMessage(buildSystemMessageConfig());
    }



    private SystemMessageConfig buildSystemMessageConfig() {
        SystemMessageConfig baseConfig = SystemMessagePresets.qwenFriendlyDefault();
        RuntimeContext runtimeContext = new RuntimeContext(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                discGlobalData.getUserProfile(),
                buildMemory(),
                buildRetrievedContext(),
                discGlobalData.getLongTermMemoryData(),
                discGlobalData.getRecentMessages(),
                discGlobalData.getGroupChatMemory(),
                baseConfig.runtimeContext().responseContract()
        );
        return SystemMessagePresets.withMessageMemory(baseConfig, runtimeContext);
    }

    private RetrievedContext buildRetrievedContext() {
        String toolSummary = toolService.executeTools(getLastAssistantMsg(), null);
        if (toolSummary == null || toolSummary.isBlank()) {
            return null;
        }
        return new RetrievedContext(null, toolSummary);
    }

    private RecentMessage getLastAssistantMsg() {
        if (discGlobalData.getRecentMessages() == null || discGlobalData.getRecentMessages().isEmpty()) return null;
        return new RecentMessage(
                discGlobalData.getRecentMessages().getLast().timestamp(),
                MessageType.ASSISTANT.toString(),
                discGlobalData.getRecentMessages().getLast().content()
        );
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