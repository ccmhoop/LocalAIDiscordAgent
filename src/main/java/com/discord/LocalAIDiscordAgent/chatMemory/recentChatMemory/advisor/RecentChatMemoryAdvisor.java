package com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.advisor;

import java.util.*;

import com.discord.LocalAIDiscordAgent.advisor.advisors.XmlAdvisor;
import com.discord.LocalAIDiscordAgent.advisor.templates.AdvisorTemplates;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.model.RecentChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.service.RecentChatMemoryService;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.util.Assert;
import reactor.core.scheduler.Scheduler;


@Getter
public final class RecentChatMemoryAdvisor extends XmlAdvisor<RecentChatMemory> implements BaseChatMemoryAdvisor {

    private final String defaultConversationId;
    private final RecentChatMemoryService service;
    private final int order;
    private final Scheduler scheduler;

    private RecentChatMemoryAdvisor(String defaultConversationId, RecentChatMemoryService service, int order, Scheduler scheduler) {
        this.service = service;
        Assert.notNull(service, "recentChatMemoryService cannot be null");
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
        Map<MessageType, List<RecentChatMemory>> chatMemories = service.getChatMemoryAsMap(conversationId);
        if (chatMemories.isEmpty()) {
            return chatClientRequest;
        }
        String oldSystemMsg = chatClientRequest.prompt().getSystemMessage().getText();
        String newSystemMsg= buildNewSystemMessage(AdvisorTemplates.SHORT_TERM_MEMORY, chatMemories, oldSystemMsg);

        return chatClientRequest.mutate().prompt(chatClientRequest.prompt().augmentSystemMessage(newSystemMsg)).build();
    }

    @Override
    @NonNull
    public ChatClientResponse after(@NonNull ChatClientResponse chatClientResponse, @NonNull AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    public static Builder builder(RecentChatMemoryService recentChatMemoryService) {
        return new Builder(recentChatMemoryService);
    }

    public static final class Builder {
        private String conversationId = "default";
        private int order = -2147482648;
        private Scheduler scheduler;
        private final RecentChatMemoryService service;

        private Builder(RecentChatMemoryService service) {
            this.service = service;
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
            return new RecentChatMemoryAdvisor(this.conversationId, this.service, this.order, this.scheduler);
        }
    }
}
