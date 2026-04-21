package com.discord.LocalAIDiscordAgent.discord.data;

import com.discord.LocalAIDiscordAgent.memory.chatMemory.groupChatMemory.service.GroupChatMemoryService;
import com.discord.LocalAIDiscordAgent.memory.chatMemory.longTermMemory.LongTermMemoryService;
import com.discord.LocalAIDiscordAgent.memory.chatMemory.recentChatMemory.service.RecentChatMemoryService;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class DiscGlobalDataService {

    private final ObjectProvider<GroupChatMemoryService> groupChatMemoryServiceProvider;
    private final ObjectProvider<RecentChatMemoryService> recentChatMemoryServiceProvider;
    private final ObjectProvider<LongTermMemoryService> longTermMemoryServiceProvider;

    public DiscGlobalDataService(
            ObjectProvider<GroupChatMemoryService> groupChatMemoryServiceProvider,
            ObjectProvider<RecentChatMemoryService> recentChatMemoryServiceProvider,
            ObjectProvider<LongTermMemoryService> longTermMemoryServiceProvider
    ) {
        this.groupChatMemoryServiceProvider = groupChatMemoryServiceProvider;
        this.recentChatMemoryServiceProvider = recentChatMemoryServiceProvider;
        this.longTermMemoryServiceProvider = longTermMemoryServiceProvider;
    }

    public Mono<DiscGlobalData> build(MessageCreateEvent event) {
        Message message = event.getMessage();
        User author = message.getAuthor().orElse(null);

        if (author == null) {
            return Mono.empty();
        }

        Snowflake botId = event.getClient().getSelfId();

        String guildId = event.getGuildId()
                .map(Snowflake::asString)
                .orElse("DM");

        String channelId = message.getChannelId().asString();
        String userId = author.getId().asString().trim();
        String username = safe(author.getUsername());
        String userGlobal = author.getGlobalName().orElse(username);
        String userMessage = extractUserMessage(message.getContent(), botId);

        Mono<String> nicknameMono = message.getAuthorAsMember()
                .map(Member::getDisplayName)
                .defaultIfEmpty(username)
                .onErrorReturn(username);

        return nicknameMono
                .map(serverNickname -> {
                    String conversationId = guildId + ":" + channelId + ":" + userId;
                    String groupConversationId = guildId + ":" + channelId;

                    return new DiscGlobalData(
                            userId,
                            guildId,
                            username,
                            channelId,
                            userGlobal,
                            userMessage,
                            serverNickname,
                            conversationId,
                            groupConversationId
                    );
                })
                .filter(DiscGlobalData::isValid)
                .flatMap(discGlobalData -> {
                    if (discGlobalData.hasEmptyPrompt()) {
                        return Mono.just(discGlobalData);
                    }

                    return Mono.fromCallable(() -> enrichMemory(discGlobalData))
                            .subscribeOn(Schedulers.boundedElastic());
                });
    }

    private DiscGlobalData enrichMemory(DiscGlobalData discGlobalData) {
        GroupChatMemoryService groupChatMemoryService = groupChatMemoryServiceProvider.getObject();
        RecentChatMemoryService recentChatMemoryService = recentChatMemoryServiceProvider.getObject();
        LongTermMemoryService longTermMemoryService = longTermMemoryServiceProvider.getObject();

        groupChatMemoryService.setDiscGlobalData(discGlobalData);
        recentChatMemoryService.setDiscGlobalData(discGlobalData);
        longTermMemoryService.setDiscGlobalData(discGlobalData);

        discGlobalData.setDiscDataMemory(
                groupChatMemoryService.buildMessageMemory(),
                recentChatMemoryService.buildMessageMemory(),
                longTermMemoryService.getLongTermMemory(),
                recentChatMemoryService.getChatMemoryAsMap()
        );

        return discGlobalData;
    }

    private String extractUserMessage(String content, Snowflake botId) {
        if (content == null || content.isBlank()) {
            return "";
        }

        return content
                .replaceAll("<@!?" + botId.asString() + ">", "")
                .trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}