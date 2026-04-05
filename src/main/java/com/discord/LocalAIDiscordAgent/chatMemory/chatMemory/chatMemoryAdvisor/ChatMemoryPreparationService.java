package com.discord.LocalAIDiscordAgent.chatMemory.chatMemory.chatMemoryAdvisor;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.chatMemory.records.ChatMemorySnapshotRecord;
import com.discord.LocalAIDiscordAgent.chatMemory.records.ChatMemorySelectionRecord.ChatMessagesSelection;
import com.discord.LocalAIDiscordAgent.chatMemory.records.ChatMemoryPayload;
import com.discord.LocalAIDiscordAgent.promptBuilderChains.data.PromptData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChatMemoryPreparationService {

    private final ChatMemorySelectionService selectionService;
    private final ChatMemoryPayloadFactory payloadFactory;

    public ChatMemoryPreparationService(
            ChatMemorySelectionService selectionService,
            ChatMemoryPayloadFactory payloadFactory
    ) {
        this.selectionService = selectionService;
        this.payloadFactory = payloadFactory;
    }

    public void prepare(DiscGlobalData discGlobalData, PromptData promptData) {

        String userMessage = discGlobalData.getUserMessage();

        if (userMessage == null || userMessage.isBlank()) {
            return;
        }

        ChatMemorySnapshotRecord memorySnapshot = new ChatMemorySnapshotRecord(
                discGlobalData.getGroupChatMemory(),
                discGlobalData.getLongTermMemoryData(),
                discGlobalData.getRecentMessages(),
                null,
                null
        );
        log.info("Memory snapshot: {}", memorySnapshot);

        boolean softRelevant = selectionService.isRelevant(memorySnapshot, userMessage);
        log.info("Soft relevance: {}", softRelevant);

        if (!softRelevant) {
            emptyPayload(promptData);
            return;
        }
        ChatMessagesSelection selectionForExtraction = payloadFactory.buildExtractionSelection(discGlobalData);

        if (selectionForExtraction == null) {
            emptyPayload(promptData);
            return;
        }

        ChatMemorySelection selection = selectionService.extractSelection(selectionForExtraction, userMessage);
        log.info("Selection: {}", selection);

        boolean hardRelevant = isUsableSelection(selection);
        log.info("Hard gate passed: {}", hardRelevant);

        if (!selectionService.isUsableSelection(selection)) {
            emptyPayload(promptData);
            return;
        }

        ChatMemoryPayload payload = payloadFactory.build(discGlobalData, selection);

        if (payload == null) {
            emptyPayload(promptData);
            return;
        }
        promptData.setChatMemoryPayload(payload);
    }

    private void emptyPayload(PromptData promptData) {
        promptData.setChatMemoryPayload(new ChatMemoryPayload(null, null, null));
    }

    private boolean isUsableSelection(ChatMemorySelection selection) {
        return selection != null
                && ((selection.ids() != null && !selection.ids().isEmpty())
                || selection.includeLongTermMemory());
    }

}