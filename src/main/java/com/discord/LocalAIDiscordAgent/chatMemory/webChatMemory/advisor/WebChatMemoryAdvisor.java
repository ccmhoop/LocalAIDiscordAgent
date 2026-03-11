package com.discord.LocalAIDiscordAgent.chatMemory.webChatMemory.advisor;

import com.discord.LocalAIDiscordAgent.advisor.advisors.QwenAdvisor;
import com.discord.LocalAIDiscordAgent.advisor.helpers.AdvisorHelper;
import com.discord.LocalAIDiscordAgent.advisor.templates.AdvisorTemplates;
import com.discord.LocalAIDiscordAgent.chatSummary.model.ChatSummary;
import com.discord.LocalAIDiscordAgent.chatMemory.webChatMemory.model.WebChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.webChatMemory.service.WebChatMemoryService;
import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
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
public class WebChatMemoryAdvisor extends QwenAdvisor<WebChatMemory> implements BaseChatMemoryAdvisor {

    private final WebChatMemoryService service;
    private final String defaultConversationId;
    private final int order;
    private final Scheduler scheduler;

    private WebChatMemoryAdvisor(WebChatMemoryService service, String defaultConversationId, int order, Scheduler scheduler) {
        super(AdvisorTemplates.WEB_SEARCH_MEMORY_TEMPLATE);
        Assert.notNull(service, "service cannot be null");
        Assert.hasText(defaultConversationId, "defaultConversationId cannot be null or empty");
        Assert.notNull(scheduler, "scheduler cannot be null");
        this.service = service;
        this.defaultConversationId = defaultConversationId;
        this.order = order;
        this.scheduler = scheduler;
        setHeader("<BEGIN_WEB_SEARCH_MEMORY>");
        setFooter("</END_WEB_SEARCH_MEMORY>");
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
        UserEntity entity = context.get("user") != null ? (UserEntity) context.get("user") : null;
        ChatSummary chatSummary = null;
        Object summaryObj = context.get("summary");
        if (summaryObj instanceof ChatSummary) {
            chatSummary = (ChatSummary) summaryObj;
        }

        setSummary(chatSummary);
        augmentSystemMsg(chatMemories, chatClientRequest.prompt().getSystemMessage().getText(), entity);
        return chatClientRequest.mutate().prompt(chatClientRequest.prompt().augmentSystemMessage(getAugmentedSystemMsg())).build();
    }


    @Override
    @NonNull
    public ChatClientResponse after(@NonNull ChatClientResponse chatClientResponse, @NonNull AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public String chatJsonBody(UserEntity userEntity, String chatMemoryData) {
        return chatMemoryData.substring(0, chatMemoryData.length() - 1);
    }

    @Override
    public String chatJsonMemory() {
        return "";
    }

    @Override
    public String chatJsonTurns(WebChatMemory user, WebChatMemory assistant) {
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

        public WebChatMemoryAdvisor build() {
            return new WebChatMemoryAdvisor(this.service, this.conversationId, this.order, this.scheduler);
        }
    }

}
