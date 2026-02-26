package com.discord.LocalAIDiscordAgent.chatMemory.webChatMemory.advisor;

import com.discord.LocalAIDiscordAgent.advisor.advisors.QwenAdvisor;
import com.discord.LocalAIDiscordAgent.advisor.helpers.AdvisorHelper;
import com.discord.LocalAIDiscordAgent.advisor.templates.AdvisorTemplates;
import com.discord.LocalAIDiscordAgent.chatMemory.webChatMemory.model.WebChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.webChatMemory.service.WebChatMemoryService;
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

import java.util.List;
import java.util.Map;

@Getter
public class WebMemoryAdvisor extends QwenAdvisor<WebChatMemory> implements BaseChatMemoryAdvisor {

    private final WebChatMemoryService service;
    private final String defaultConversationId;
    private final int order;
    private final Scheduler scheduler;

    private WebMemoryAdvisor(WebChatMemoryService service, String defaultConversationId, int order, Scheduler scheduler) {
        super(AdvisorTemplates.WEB_SEARCH_MEMORY_TEMPLATE);
        Assert.notNull(service, "service cannot be null");
        Assert.hasText(defaultConversationId, "defaultConversationId cannot be null or empty");
        Assert.notNull(scheduler, "scheduler cannot be null");
        this.service = service;
        this.defaultConversationId = defaultConversationId;
        this.order = order;
        this.scheduler = scheduler;
    }

    @Override
    @NonNull
    public ChatClientRequest before(ChatClientRequest chatClientRequest, @NonNull AdvisorChain advisorChain) {
        Map<String, Object> context = chatClientRequest.context();
        String userId = this.getConversationId(chatClientRequest.context(), this.defaultConversationId);
        Map<MessageType, List<WebChatMemory>> chatMemories = service.getChatMemoryAsMap(userId);
        if (chatMemories.isEmpty()) {
            return chatClientRequest;
        }
        augmentSystemMsg(chatMemories, chatClientRequest.prompt().getSystemMessage().getText());
        return chatClientRequest.mutate().prompt(chatClientRequest.prompt().augmentSystemMessage(getAugmentedSystemMsg())).build();
    }

    @Override
    @NonNull
    public ChatClientResponse after(@NonNull ChatClientResponse chatClientResponse, @NonNull AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public String chatMemoryBody(WebChatMemory user, String chatMemoryData) {
        return chatMemoryData.substring(0, chatMemoryData.length() - 1);
    }

    @Override
    public String chatMemoryData(WebChatMemory user, WebChatMemory assistant) {
        return """
                {
                "user.sent": "%s",
                "assistant".respond: "%s"
                }
                """.formatted(
                AdvisorHelper.indentLines(user.getContent(),0),
                AdvisorHelper.indentLines(assistant.getContent(),0));
    }

    public static Builder builder(WebChatMemoryService service) {
        return new Builder(service);
    }

    public static final class Builder {
        private String conversationId = "default";
        private int order = -2147482648;
        private Scheduler scheduler;
        private final WebChatMemoryService service;

        private Builder(WebChatMemoryService service) {
            this.scheduler = BaseAdvisor.DEFAULT_SCHEDULER;
            this.service = service;
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

        public WebMemoryAdvisor build() {
            return new WebMemoryAdvisor(this.service, this.conversationId, this.order, this.scheduler);
        }
    }

}
