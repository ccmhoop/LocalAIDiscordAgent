package com.discord.LocalAIDiscordAgent.discord.data;

import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.service.GroupChatMemoryService;
import com.discord.LocalAIDiscordAgent.chatMemory.longTermMemory.LongTermMemoryService;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.service.RecentChatMemoryService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class DiscGlobalDataService {

    private final ObjectProvider<DiscGlobalData> discGlobalDataProvider;
    private final ObjectProvider<GroupChatMemoryService> groupChatMemoryServiceProvider;
    private final ObjectProvider<RecentChatMemoryService> recentChatMemoryServiceProvider;
    private final ObjectProvider<LongTermMemoryService> longTermMemoryServiceProvider;

    public DiscGlobalDataService(
            ObjectProvider<DiscGlobalData> discGlobalDataProvider,
            ObjectProvider<GroupChatMemoryService> groupChatMemoryServiceProvider,
            ObjectProvider<RecentChatMemoryService> recentChatMemoryServiceProvider,
            ObjectProvider<LongTermMemoryService> longTermMemoryServiceProvider
    ) {
        this.discGlobalDataProvider = discGlobalDataProvider;
        this.groupChatMemoryServiceProvider = groupChatMemoryServiceProvider;
        this.recentChatMemoryServiceProvider = recentChatMemoryServiceProvider;
        this.longTermMemoryServiceProvider = longTermMemoryServiceProvider;
    }

    public DiscGlobalData build(MessageCreateEvent event) {
        DiscGlobalData discGlobalData = discGlobalDataProvider.getObject();

        boolean populated = discGlobalData.setDiscData(event);

        if (!populated || !discGlobalData.isValid()) {
            return discGlobalData;
        }

        if (discGlobalData.hasEmptyPrompt()) {
            return discGlobalData;
        }

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
}