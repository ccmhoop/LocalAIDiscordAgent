package com.discord.LocalAIDiscordAgent.Advisor.advisor;

import java.util.*;

import com.discord.LocalAIDiscordAgent.Advisor.templates.AdvisorTemplates;
import com.discord.LocalAIDiscordAgent.chatMemory.model.RecentChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.service.RecentChatMemoryService;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.util.Assert;
import reactor.core.scheduler.Scheduler;

@Getter
public final class RecentChatMemoryAdvisor implements BaseChatMemoryAdvisor {

    private final String defaultConversationId;
    private final RecentChatMemoryService recentChatMemoryService;
    private final int order;
    private final Scheduler scheduler;

    /*
     * @Todo
     *   Make this configurable by adding a property to the application.properties file.
     *   Add a method to the RecentChatMemoryService to limit the number of messages returned by the getAllAndSort method.
     *   Add a method to the RecentChatMemoryService to limit X amount of messages in the chat memory depending on the user.
     */
    private final int TEMPORARY_MEMORY_SIZE = 6;


    private RecentChatMemoryAdvisor(String defaultConversationId, RecentChatMemoryService recentChatMemoryService, int order, Scheduler scheduler) {
        this.recentChatMemoryService = recentChatMemoryService;
        Assert.notNull(recentChatMemoryService, "recentChatMemoryService cannot be null");
        Assert.hasText(defaultConversationId, "defaultConversationId cannot be null or empty");
        Assert.notNull(scheduler, "scheduler cannot be null");
        this.defaultConversationId = defaultConversationId;
        this.order = order;
        this.scheduler = scheduler;
    }

    @Override
    @NonNull
    public ChatClientRequest before(ChatClientRequest chatClientRequest, @NonNull AdvisorChain advisorChain) {
        String conversationId = this.getConversationId(chatClientRequest.context(), this.defaultConversationId);
        Map<MessageType, List<RecentChatMemory>> chatMemories = recentChatMemoryService.getAllAndSort(conversationId);
        SystemMessage previousSystemMsg = chatClientRequest.prompt().getSystemMessage();
        String newSystemMessage = previousSystemMsg.getText() + buildPromptTemplate(conversationId, chatMemories);
        return chatClientRequest.mutate().prompt(chatClientRequest.prompt().augmentSystemMessage(newSystemMessage)).build();
    }

    @Override
    @NonNull
    public ChatClientResponse after(@NonNull ChatClientResponse chatClientResponse, @NonNull AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    private String buildPromptTemplate(String conversationId, Map<MessageType, List<RecentChatMemory>> chatMemories) {
        return PromptTemplate.builder()
                .template(AdvisorTemplates.SHORT_TERM_MEMORY.getTemplate())
                .variables(Map.of(
                        "recent_chat_memory", buildRecentChat(conversationId, chatMemories)
                ))
                .build()
                .render();
    }


    private String buildRecentChat(String userId, Map<MessageType, List<RecentChatMemory>> chatMemories) {
        if (chatMemories.get(MessageType.USER).isEmpty() || chatMemories.get(MessageType.ASSISTANT).isEmpty()) {
            return "None";
        }

        List<RecentChatMemory> userMemories = chatMemories.get(MessageType.USER);
        List<RecentChatMemory> assistantMemories = chatMemories.get(MessageType.ASSISTANT);

        int from = Math.max(0, assistantMemories.size() - TEMPORARY_MEMORY_SIZE);
        assistantMemories = assistantMemories.subList(from, assistantMemories.size());
        userMemories = userMemories.subList(from, userMemories.size());

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < assistantMemories.size(); i++) {
            RecentChatMemory userChat = userMemories.get(i);
            RecentChatMemory assistantChat = assistantMemories.get(i);
            sb.append(formatRecentChat(userId, userChat, assistantChat));
        }

        return sb.toString().stripTrailing();
    }

    private String formatRecentChat(String userId, RecentChatMemory userChat, RecentChatMemory assistantChat) {
        return """
                <message_pair>
                \t<note>
                \t\t<date>%s</date>
                \t\t<to>assistant</to>
                \t\t<from>%s</from>
                \t\t<body>
                %s
                \t\t</body>
                \t/<note>
                \t<note>
                \t\t<date>%s</date>
                \t\t<to>%s</to>
                \t\t<from>assistant</from>
                \t\t<body>
                %s
                \t\t</body>
                \t</note>
                </message_pair>
                """
                .formatted(
                        userChat.getTimestamp(),
                        userId,
                        indentBlock(userChat.getContent()),
                        assistantChat.getTimestamp(),
                        userId,
                        indentBlock(assistantChat.getContent()));
    }

    private static String indentBlock(String s) {
        if (s == null || s.isBlank()) return "";
        String[] lines = s.split("\\R", -1);
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            if (line.isBlank()) continue;
            out.append("\t\t\t").append(line.stripLeading()).append("\n");
        }
        return out.toString().stripTrailing();
    }

    public static Builder builder(RecentChatMemoryService recentChatMemoryService) {
        return new Builder(recentChatMemoryService);
    }

    public static final class Builder {
        private String conversationId = "default";
        private int order = -2147482648;
        private Scheduler scheduler;
        private final RecentChatMemoryService recentChatMemoryService;

        private Builder(RecentChatMemoryService recentChatMemoryService) {
            this.recentChatMemoryService = recentChatMemoryService;
            this.scheduler = BaseAdvisor.DEFAULT_SCHEDULER;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public RecentChatMemoryAdvisor build() {
            return new RecentChatMemoryAdvisor(this.conversationId, this.recentChatMemoryService, this.order, this.scheduler);
        }
    }
}
