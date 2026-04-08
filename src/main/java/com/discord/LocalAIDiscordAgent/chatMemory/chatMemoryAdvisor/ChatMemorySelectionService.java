package com.discord.LocalAIDiscordAgent.chatMemory.chatMemoryAdvisor;

import com.discord.LocalAIDiscordAgent.chatMemory.records.ChatMemorySnapshotRecord;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class ChatMemorySelectionService {

    private final MapperUtils  mapperUtils;

    private static final String RELEVANCE_SYSTEM_MESSAGE = """
            You are a strict relevance classifier.

            Your task is to decide whether the chat memory inside <memory> is relevant to the user_message.

            Rules:
            1. If <memory> is empty, return false.
            2. Be conservative. If relevance or usefulness is uncertain, weak, partial, or ambiguous, return false.
            3. Do not return true based on keyword overlap alone.
            4. Return true only when the chat memory matches the user_message in a meaningful way.
            5. be lenient. If the chat memory is relevant, but the user_message is not, return true.
            
            Return only one word: true or false.

            <memory>
            %s
            </memory>
            """;

//            5. Prefer false over true unless the chat memory would clearly improve the answer.


    private static final String EXTRACTION_SYSTEM_MESSAGE = """
            You are a strict chat memory extraction assistant.

            Your task is to analyze the <memory> and extract ids of valuable messages for contextualization based on the user_message:
            - ids: the message IDs to include in the output
            - includeLongTermMemory: whether long-term memory should be included

            Rules:
            1. Extract only explicit message IDs from the <memory>.
            2. Add each valid id to the ids list.
            3. Do not infer, guess, or invent missing IDs.
            4. Decide includeLongTermMemory only if it is valuable for contextualization.
            5. Return structured output only.

            <memory>
            %s
            </memory>
            """;

    private final ChatClient internalChatClient;

    public ChatMemorySelectionService(MapperUtils mapperUtils, ChatModel structuredLLMModel) {
        this.mapperUtils = mapperUtils;
        this.internalChatClient = ChatClient.builder(structuredLLMModel).build();
    }

    public boolean isRelevant(ChatMemorySnapshotRecord memorySnapshot, String userMessage) {
        String relevanceText = internalChatClient.prompt()
                .system(RELEVANCE_SYSTEM_MESSAGE.formatted(mapperUtils.valuesToString(memorySnapshot)))
                .user("""
                        User message:
                        --------------------------
                        %s
                        --------------------------
                        Return only one word: true or false.
                        """.formatted(userMessage))
                .call()
                .content();

        return Boolean.parseBoolean(
                Optional.ofNullable(relevanceText)
                        .orElse("false")
                        .trim()
                        .toLowerCase(Locale.ROOT)
        );
    }

    public ChatMemorySelection extractSelection(Record memorySnapshot, String userMessage) {
        return internalChatClient.prompt()
                .system(EXTRACTION_SYSTEM_MESSAGE.formatted(
                        mapperUtils.valuesToString(memorySnapshot)
                ))
                .user("""
                        Extract the valuable message ids for contextualization based on the user message.
                        Always return at least one id.

                        User message:
                        --------------------------
                        %s
                        --------------------------
                        """.formatted(userMessage))
                .call()
                .entity(ChatMemorySelection.class);
    }

    public boolean isUsableSelection(ChatMemorySelection selection) {
        return selection != null
                && ((selection.ids() != null && !selection.ids().isEmpty())
                || selection.includeLongTermMemory());
    }
}