package com.discord.LocalAIDiscordAgent.discord.service;

import com.discord.LocalAIDiscordAgent.chatMemory.groupChatMemory.service.GroupChatMemoryService;
import com.discord.LocalAIDiscordAgent.chatMemory.recentChatMemory.service.RecentChatMemoryService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.discord.listener.EventListenerINTF;
import com.discord.LocalAIDiscordAgent.discord.listener.MessageListener;
import com.discord.LocalAIDiscordAgent.chatClient.service.ChatClientService;
import com.discord.LocalAIDiscordAgent.interactionProcessor.ProcessSummaryClient;
import com.discord.LocalAIDiscordAgent.user.service.UserService;
import com.discord.LocalAIDiscordAgent.vectorMemory.longTermMemory.LongTermMemoryService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class MessageCreateListenerINTF extends MessageListener implements EventListenerINTF<MessageCreateEvent> {

    private final UserService userService;
    private final DiscGlobalData discGlobalData;
    private final ChatClientService chatClientService;
    private final ProcessSummaryClient processSummaryClient;
    private final RecentChatMemoryService recentChatMemoryService;
    private final GroupChatMemoryService groupChatMemoryService;
    private final LongTermMemoryService longTermMemoryService;


    public MessageCreateListenerINTF(
            UserService userService,
            DiscGlobalData discGlobalData,
            ChatClientService chatClientService,
            ProcessSummaryClient processSummaryClient,
            LongTermMemoryService longTermMemoryService,
            GroupChatMemoryService groupChatMemoryService,
            RecentChatMemoryService recentChatMemoryService
    ) {
        this.recentChatMemoryService = recentChatMemoryService;
        this.groupChatMemoryService = groupChatMemoryService;
        this.longTermMemoryService = longTermMemoryService;
        this.processSummaryClient = processSummaryClient;
        this.chatClientService = chatClientService;
        this.discGlobalData = discGlobalData;
        this.userService = userService;
    }

    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {

        discGlobalData.setDiscTonull();


        discGlobalData.setDiscData(event);

        if (discGlobalData.dataIsEmptyOrNull() || discGlobalData.getUserMessage().isBlank()){
            return Mono.empty();
        }

        discGlobalData.setDiscDataMemory(
                groupChatMemoryService.buildMessageMemory(),
                recentChatMemoryService.buildMessageMemory(),
                longTermMemoryService.getLongTermMemory()
        );


        return processCommandAI(event.getMessage(), discGlobalData, userService, chatClientService, processSummaryClient);
    }

}
