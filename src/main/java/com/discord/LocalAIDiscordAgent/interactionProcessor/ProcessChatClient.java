package com.discord.LocalAIDiscordAgent.interactionProcessor;

import com.discord.LocalAIDiscordAgent.chatMemory.chatMemory.groupChatMemory.service.GroupChatMemoryService;
import com.discord.LocalAIDiscordAgent.chatMemory.chatMemory.longTermMemory.LongTermMemoryService;
import com.discord.LocalAIDiscordAgent.chatMemory.chatMemory.recentChatMemory.service.RecentChatMemoryService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalDataContextHolder;
import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Component
public class ProcessChatClient {

    private final ObjectProvider<GroupChatMemoryService> groupServiceProvider;
    private final ObjectProvider<RecentChatMemoryService> recentServiceProvider;
    private final ObjectProvider<LongTermMemoryService> longTermMemoryServiceProvider;

    public ProcessChatClient(
            ObjectProvider<RecentChatMemoryService> recentServiceProvider,
            ObjectProvider<GroupChatMemoryService> groupServiceProvider,
            ObjectProvider<LongTermMemoryService> longTermMemoryServiceProvider
    ) {
        this.recentServiceProvider = recentServiceProvider;
        this.groupServiceProvider = groupServiceProvider;
        this.longTermMemoryServiceProvider = longTermMemoryServiceProvider;
    }

    public Mono<Void> saveInteraction(
            UserMessage userMessage,
            AssistantMessage assistantMessage,
            UserEntity userEntity
    ) {
        return DiscGlobalDataContextHolder.get()
                .flatMap(discGlobalData ->
                        Mono.fromRunnable(() -> {
                                    List<Message> messages = List.of(userMessage, assistantMessage);

                                    RecentChatMemoryService recentService = recentServiceProvider.getObject();
                                    GroupChatMemoryService groupService = groupServiceProvider.getObject();
                                    LongTermMemoryService longTermMemoryService = longTermMemoryServiceProvider.getObject();

                                    recentService.setDiscGlobalData(discGlobalData);
                                    groupService.setDiscGlobalData(discGlobalData);
                                    longTermMemoryService.setDiscGlobalData(discGlobalData);

                                    recentService.saveAndTrim(messages, userEntity);
                                    groupService.saveAndTrim(messages, userEntity);
                                    longTermMemoryService.saveLongTermMemory(assistantMessage);
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                ).then();
    }
}