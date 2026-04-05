package com.discord.LocalAIDiscordAgent.discord.service;

import com.discord.LocalAIDiscordAgent.chatMemory.chatMemory.groupChatMemory.service.GroupChatMemoryService;
import com.discord.LocalAIDiscordAgent.chatMemory.chatMemory.recentChatMemory.model.RecentChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.chatMemory.recentChatMemory.service.RecentChatMemoryService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.discord.listener.EventListenerINTF;
import com.discord.LocalAIDiscordAgent.discord.listener.MessageListener;
import com.discord.LocalAIDiscordAgent.llmClients.chatClient.service.ChatClientService;
import com.discord.LocalAIDiscordAgent.interactionProcessor.ProcessSummaryClient;
import com.discord.LocalAIDiscordAgent.user.service.UserService;
import com.discord.LocalAIDiscordAgent.chatMemory.chatMemory.longTermMemory.LongTermMemoryService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;
import static org.springframework.ai.chat.messages.MessageType.USER;

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

        if (discGlobalData.getUserMessage() == null || discGlobalData.getUserMessage().isBlank()){
            return Mono.empty();
        }

        Map<MessageType, List<RecentChatMemory>> recentMap = recentChatMemoryService.getChatMemoryAsMap();

        List<RecentChatMemory> recentUsers = recentMap.getOrDefault(USER, List.of());
        List<RecentChatMemory> recentAssistants = recentMap.getOrDefault(ASSISTANT, List.of());

        discGlobalData.setDiscDataMemory(
                groupChatMemoryService.buildMessageMemory(),
                recentChatMemoryService.buildMessageMemory(),
                longTermMemoryService.getLongTermMemory(),
                recentChatMemoryService.getChatMemoryAsMap()
        );


        return processCommandAI(event.getMessage(), discGlobalData, userService, chatClientService, processSummaryClient);
    }

}
