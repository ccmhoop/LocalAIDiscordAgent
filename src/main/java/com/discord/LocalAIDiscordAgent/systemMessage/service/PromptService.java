package com.discord.LocalAIDiscordAgent.systemMessage.service;

import com.discord.LocalAIDiscordAgent.chatSummary.model.ChatSummary;
import com.discord.LocalAIDiscordAgent.chatSummary.repository.ChatSummaryRepository;
import com.discord.LocalAIDiscordAgent.chatSummary.records.SummaryRecords.Fact;
import com.discord.LocalAIDiscordAgent.chatClient.helpers.ChatClientHelpers;
import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.service.GroupChatMemoryService;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.service.RecentChatMemoryService;
import com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey;
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
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class PromptService {

    private final SystemMessageFactory systemMessageFactory;
    private final RecentChatMemoryService recentChatService;
    private final GroupChatMemoryService groupChatService;
    private final ChatSummaryRepository repo;

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    public PromptService(SystemMessageFactory systemMessageFactory, RecentChatMemoryService recentChatMemoryService, GroupChatMemoryService groupChatMemoryService, ChatSummaryRepository repo) {
        this.systemMessageFactory = systemMessageFactory;
        this.recentChatService = recentChatMemoryService;
        this.groupChatService = groupChatMemoryService;
        this.repo = repo;
    }

    public String buildSystemPrompt() {
        SystemMessageConfig config = SystemMessagePresets.qwenFriendlyDefault();
        return systemMessageFactory.buildSystemMessage(config);
    }

    public String buildSystemMsgJson(Map<DiscDataKey, String> discDataMap) {
        SystemMessageConfig baseConfig = SystemMessagePresets.qwenFriendlyDefault();

        String recentConversationId = ChatClientHelpers.buildConversationId(discDataMap);
        String groupConversationId = ChatClientHelpers.buildGroupConversationId(discDataMap);

        UserProfile userProfile = buildUserProfile(discDataMap);

        List<RecentMessage> recentMessages = recentChatService.buildMessageMemory(recentConversationId);
        GroupMemory groupChatMemory = groupChatService.buildMessageMemory(groupConversationId);

        Memory baseMemory;

        if (groupChatMemory == null) {
            baseMemory = buildMemory(groupConversationId);
        }else{
            baseMemory = buildMemory(recentConversationId);
        }

        baseConfig = SystemMessagePresets.withMessageMemory(
                baseConfig,
                userProfile,
                baseMemory,
                recentMessages,
                groupChatMemory,
                discDataMap.get(DiscDataKey.USER_MESSAGE)
        );

        return systemMessageFactory.buildSystemMessage(baseConfig);

    }

    private UserProfile buildUserProfile(Map<DiscDataKey, String> discDataMap){
        return new UserProfile(
                discDataMap.get(DiscDataKey.USER_ID),
                discDataMap.get(DiscDataKey.USERNAME),
                discDataMap.get(DiscDataKey.SERVER_NICKNAME));
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