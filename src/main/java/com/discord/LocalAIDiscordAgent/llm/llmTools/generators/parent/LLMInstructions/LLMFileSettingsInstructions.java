package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.LLMInstructions;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public abstract class LLMFileSettingsInstructions<T extends Record> {

    private final String systemsMessage;
    private final String userInstruction;
    private ChatClient internalChatClient;
    private final Class<T> fileSettingsDTO;

    @Autowired
    @Qualifier("chatModelFileSettingsGenerator")
    private ChatModel chatModel;

    @PostConstruct
    private void init() {
        this.internalChatClient = setInternalChatClient();
    }

    protected LLMFileSettingsInstructions(Class<T> fileSettingsDTO, String systemsMessage, String userInstruction) {
        this.fileSettingsDTO = fileSettingsDTO;
        this.systemsMessage = systemsMessage;
        this.userInstruction = userInstruction;
    }

    public T generateSettings(String userMessage, String context) {
        String safeContext = context == null ? "" : context.trim();

        return internalChatClient.prompt()
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .system(systemsMessage.formatted(safeContext))
                .user(userInstruction.formatted(userMessage))
                .call()
                .entity(fileSettingsDTO);
    }

    private ChatClient setInternalChatClient() {
        var converter = new BeanOutputConverter<>(fileSettingsDTO);

        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(fileSettingsDTO)
                .maxRepeatAttempts(3)
                .build();

        return ChatClient.builder(chatModel)
                .defaultOptions(OllamaChatOptions.builder()
                        .format(converter.getJsonSchemaMap())
                        .build())
                .defaultAdvisors(validation)
                .build();
    }

}
