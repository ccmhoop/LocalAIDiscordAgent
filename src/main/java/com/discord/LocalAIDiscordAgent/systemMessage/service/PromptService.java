package com.discord.LocalAIDiscordAgent.systemMessage.service;

import com.discord.LocalAIDiscordAgent.chatSummary.model.ChatSummary;
import com.discord.LocalAIDiscordAgent.chatSummary.repository.ChatSummaryRepository;
import com.discord.LocalAIDiscordAgent.chatSummary.records.SummaryRecords.Fact;
import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.service.GroupChatMemoryService;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.service.RecentChatMemoryService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.systemMessage.SystemMessageFactory;
import com.discord.LocalAIDiscordAgent.systemMessage.SystemMessagePresets;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.*;
import com.discord.LocalAIDiscordAgent.webQA.service.WebQAService;
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

    private final SystemMessageFactory systemMessageFactory;
    private final RecentChatMemoryService recentChatService;
    private final GroupChatMemoryService groupChatService;
    private final WebQAService webQAService;
    private final ChatSummaryRepository repo;
    private final DiscGlobalData discGlobalData;

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    public PromptService(SystemMessageFactory systemMessageFactory, RecentChatMemoryService recentChatMemoryService, GroupChatMemoryService groupChatMemoryService, WebQAService webQAService, ChatSummaryRepository repo, DiscGlobalData discGlobalData) {
        this.systemMessageFactory = systemMessageFactory;
        this.recentChatService = recentChatMemoryService;
        this.groupChatService = groupChatMemoryService;
        this.webQAService = webQAService;
        this.repo = repo;
        this.discGlobalData = discGlobalData;
    }

    public String buildSystemPrompt() {
        SystemMessageConfig config = SystemMessagePresets.qwenFriendlyDefault();
        return systemMessageFactory.buildSystemMessage(config);
    }

    public String buildSystemMsgJson() {
        SystemMessageConfig baseConfig = SystemMessagePresets.qwenFriendlyDefault();

        List<RecentMessage> recentMessages = recentChatService.buildMessageMemory();
        GroupMemory groupChatMemory = groupChatService.buildMessageMemory();

        Memory baseMemory;

        if (groupChatMemory == null) {
            baseMemory = buildMemory(discGlobalData.getGroupConversationId());
        }else{
            baseMemory = buildMemory(discGlobalData.getConversationId());
        }

        if (baseMemory == null) {
            return "";
        }

        RetrievedContext retrievedContext = new RetrievedContext(webQAService.getWebQAResults());

        if (retrievedContext.webResults() == null || retrievedContext.webResults().isEmpty()) {
            retrievedContext = null;
        }

        RuntimeContext runtimeContext = new RuntimeContext(
                buildUserProfile(),
                baseMemory,
                retrievedContext,
                recentMessages,
                groupChatMemory,
                discGlobalData.getUserMessage(),
                baseConfig.runtimeContext().responseContract()

        );

        baseConfig = SystemMessagePresets.withMessageMemory(
                baseConfig,
                runtimeContext
        );

        return systemMessageFactory.buildSystemMessage(baseConfig);
    }

    private UserProfile buildUserProfile(){
        return new UserProfile(
                discGlobalData.getUserId(),
                discGlobalData.getUsername(),
                discGlobalData.getServerNickname());
    }

    private Memory buildMemory(String conversationId) {
        try {
            Optional<ChatSummary> mem = repo.findById(conversationId);
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