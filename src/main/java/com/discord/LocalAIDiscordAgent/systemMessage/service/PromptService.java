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
import com.discord.LocalAIDiscordAgent.toolClient.service.ToolService;
import com.discord.LocalAIDiscordAgent.webQA.service.WebQAService;
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
    private final WebQAService webQAService;
    private final ChatSummaryRepository repo;
    private final DiscGlobalData discGlobalData;
    private final GroupChatMemoryService groupChatService;
    private final RecentChatMemoryService recentChatService;
    private final SystemMessageFactory systemMessageFactory;

    private List<RecentMessage> recentMessages;
    private RetrievedContext retrievedContext;
    private GroupMemory groupChatMemory;
    private UserProfile userProfile;
    private Memory baseMemory;


    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    public PromptService(
            ToolService toolService,
            WebQAService webQAService,
            ChatSummaryRepository repo,
            DiscGlobalData discGlobalData,
            SystemMessageFactory systemMessageFactory,
            GroupChatMemoryService groupChatMemoryService,
            RecentChatMemoryService recentChatMemoryService
    ) {
        this.systemMessageFactory = systemMessageFactory;
        this.recentChatService = recentChatMemoryService;
        this.groupChatService = groupChatMemoryService;
        this.discGlobalData = discGlobalData;
        this.webQAService = webQAService;
        this.toolService = toolService;
        this.repo = repo;
    }

    public String buildSystemPrompt() {
        SystemMessageConfig config = SystemMessagePresets.qwenFriendlyDefault();
        return systemMessageFactory.buildSystemMessage(config);
    }

    public String getSystemPromptAsJson() {
        this.userProfile = buildUserProfile();
        this.recentMessages = recentChatService.buildMessageMemory();
        this.groupChatMemory = groupChatService.buildMessageMemory();

        if (groupChatMemory != null) {
            this.baseMemory = buildMemory(discGlobalData.getConversationId());
        } else {
            this.baseMemory = buildMemory(discGlobalData.getGroupConversationId());
        }

        List<MergedWebQAItem> webQAResults = webQAService.getWebQAResults( baseMemory, recentMessages);

        String toolSummary = toolService.executeTools(userProfile, getLastAssistantMsg(), webQAResults);

        if (toolSummary == null || toolSummary.isBlank()) {
            retrievedContext = null;
        }else{
            this.retrievedContext = new RetrievedContext(null, toolSummary);
        }

        return systemMessageFactory.buildSystemMessage(buildSystemMessageConfig());
    }

    public String buildSystemPromptJson(SystemMessageConfig config) {
        return systemMessageFactory.buildSystemMessage(config);
    }

    private SystemMessageConfig buildSystemMessageConfig() {
        SystemMessageConfig baseConfig = SystemMessagePresets.qwenFriendlyDefault();
        RuntimeContext runtimeContext = new RuntimeContext(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                this.userProfile,
                this.baseMemory,
                this.retrievedContext,
                this.recentMessages,
                this.groupChatMemory,
                baseConfig.runtimeContext().responseContract()
        );

        return SystemMessagePresets.withMessageMemory(baseConfig, runtimeContext);
    }

    private RecentMessage getLastAssistantMsg() {
        if (this.recentMessages == null || this.recentMessages.isEmpty()) return null;
        return new RecentMessage(
                this.recentMessages.getLast().timestamp(),
                MessageType.ASSISTANT.toString(),
                this.recentMessages.getLast().content()
        );
    }

    private UserProfile buildUserProfile() {
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