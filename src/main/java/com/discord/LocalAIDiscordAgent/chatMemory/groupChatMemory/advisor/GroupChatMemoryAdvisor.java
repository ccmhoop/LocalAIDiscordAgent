package com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.advisor;

import com.discord.LocalAIDiscordAgent.advisor.advisors.QwenAdvisor;
import com.discord.LocalAIDiscordAgent.advisor.helpers.AdvisorHelper;
import com.discord.LocalAIDiscordAgent.advisor.templates.AdvisorTemplates;
import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.model.GroupChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.service.GroupChatMemoryService;
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
public class GroupChatMemoryAdvisor extends QwenAdvisor<GroupChatMemory> implements BaseChatMemoryAdvisor {

    private final String defaultConversationId;
    private final GroupChatMemoryService service;
    private final int order;
    private final Scheduler scheduler;

    private GroupChatMemoryAdvisor(String defaultConversationId, GroupChatMemoryService service, int order, Scheduler scheduler) {
        super(AdvisorTemplates.GROUP_CHAT_MEMORY);
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
    public ChatClientRequest before(@NonNull ChatClientRequest chatClientRequest, @NonNull AdvisorChain advisorChain) {
        Map<String, Object> context = chatClientRequest.context();
        String guildId = context.get("guild_id").toString();
        Map<MessageType, List<GroupChatMemory>> chatMemories = service.getChatMemoryAsMap(guildId);
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
    public String chatMemoryBody(GroupChatMemory user, String chatMemoryData) {
        return chatMemoryData.substring(0, chatMemoryData.length() - 2);
    }

    @Override
    public String chatMemoryData(GroupChatMemory user, GroupChatMemory assistant) {
        String dateTime = user.getTimestamp().toString();
        return """
                {
                "user.globalName": "%s",
                "user.nickname": "%s",
                "user.mention": "<@%s>",
                "message.data":{
                    "date": "%s",
                    "time": "%s",
                    "user.sent": "%s",
                    "assistant.respond": respond: "%s"
                    }
                },
                """.formatted(
                user.getUser().getUserGlobal(),
                user.getUser().getServerNickname(),
                user.getUser().getUserId(),
                dateTime.substring(0, dateTime.indexOf("T")),
                dateTime.substring(dateTime.indexOf("T") + 1),
                AdvisorHelper.indentLines(assistant.getContent(),0),
                AdvisorHelper.indentLines(assistant.getContent(),0)
        );
    }

    public static Builder builder(GroupChatMemoryService service) {
        return new Builder(service);
    }

    public static final class Builder {
        private String conversationId = "default";
        private int order = -2147482648;
        private Scheduler scheduler;
        private final GroupChatMemoryService service;

        private Builder(GroupChatMemoryService service) {
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

        public GroupChatMemoryAdvisor build() {
            return new GroupChatMemoryAdvisor(this.conversationId, this.service, this.order, this.scheduler);
        }
    }

}
