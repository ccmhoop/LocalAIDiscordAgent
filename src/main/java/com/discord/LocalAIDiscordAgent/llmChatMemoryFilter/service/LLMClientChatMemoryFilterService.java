package com.discord.LocalAIDiscordAgent.llmChatMemoryFilter.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmChatMemoryFilter.records.ChatMemoryFilter;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.GroupMemory;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RuntimeContext;
import com.discord.LocalAIDiscordAgent.vectorMemory.longTermMemory.LongTermMemoryService.LongTermMemoryData;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class LLMClientChatMemoryFilterService {

    private final MapperUtils mapperUtils;
    private final DiscGlobalData discGlobalData;
    private final ChatClient llmFilterClient;

    private InstructionsRecord instructionRecord;
    private Record context;
    private FilteredChatMemoryRecord filteredChatMemoryRecord;
    private List<String> instructions;
    private String systemMsg;
    private Prompt prompt;


    public LLMClientChatMemoryFilterService(
            MapperUtils mapperUtils,
            DiscGlobalData discGlobalData,
            ChatClient isValidToolClient
    ) {
        this.llmFilterClient = isValidToolClient;
        this.discGlobalData = discGlobalData;
        this.mapperUtils = mapperUtils;
    }

    public RuntimeContext preformFilter(@NonNull ChatMemoryFilter chatMemoryFilter) {
        this.context = chatMemoryFilter.chatMemoryContext();
        this.instructions = chatMemoryFilter.instruction();
        this.systemMsg = chatMemoryFilter.systemMsg();
        setInstructionsRecord();
        setPrompt();
        log.info("LLM filter prompt: {}", prompt);

        FilteredChatMemoryRecord filteredChatMemoryRecord = callLLM();
        if (filteredChatMemoryRecord == null) {
            return null;
        }

        return new RuntimeContext(
                null,
                null,
                null,
                null,
                null,
                filteredChatMemoryRecord.recentMessages(),
                null,
                null
        );
    }


    private void setInstructionsRecord() {
        this.instructionRecord = new InstructionsRecord(
                instructions,
                context,
                discGlobalData.getUserMessage()
        );
    }

    public record InstructionsRecord(
            List<String> instructions,
            Record retrievedContext,
            String userMessage
    ) {
    }

    private void setPrompt() {
        this.prompt = Prompt.builder()
                .messages(
                        buildSystemMessageJson(),
                        buildUserMessageJson()
                )
                .build();
    }

    private SystemMessage buildSystemMessageJson() {
        String objectJsonSchema = mapperUtils.objectTypeTostring(instructionRecord);
        this.systemMsg = this.systemMsg.formatted(objectJsonSchema);
        return new SystemMessage(this.systemMsg);
    }

    private UserMessage buildUserMessageJson() {
        String userMessage = mapperUtils.valuesToString(instructionRecord);
        return new UserMessage(userMessage);
    }

    private FilteredChatMemoryRecord callLLM() {
        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(FilteredChatMemoryRecord.class)
                .maxRepeatAttempts(3)
                .build();

        var converter = new BeanOutputConverter<>(FilteredChatMemoryRecord.class);

        return llmFilterClient.prompt(this.prompt)
                .options(OllamaChatOptions.builder()
                        .format(converter.getJsonSchema())
                        .build())
                .advisors(validation)
                .call()
                .entity(FilteredChatMemoryRecord.class);
    }

    public record FilteredChatMemoryRecord(
//            List<LongTermMemoryData> longTermMemory,
//            GroupMemory groupMemory,
            List<RecentMessage> recentMessages
    ) {}
}
