package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.generator;

import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;

public abstract class LLMSettingsGenerator<T extends Record> {

    private final Class<T> recordClass;
    private final String systemsMessage;
    private final String userInstruction;
    private final ChatClient internalChatClient;

    public LLMSettingsGenerator(Class<T> recordClass, ChatModel llmPayloadModel) {
        assert recordClass != null;
        assert llmPayloadModel != null;
        this.recordClass = recordClass;
        this.systemsMessage = setSystemMessage();
        this.userInstruction = setUserInstruction();
        this.internalChatClient = setInternalChatClient(llmPayloadModel);
    }

    public abstract String setSystemMessage();
    public abstract String setUserInstruction();

    public T generatePayload(String userMessage, String context) {
        String safeContext = context == null ? "" : context.trim();

        return internalChatClient.prompt()
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .system(systemsMessage.formatted(safeContext))
                .user(userInstruction.formatted(userMessage))
                .call()
                .entity(recordClass);
    }

    private ChatClient setInternalChatClient(ChatModel llmPayloadModel) {

        var converter = new BeanOutputConverter<>(recordClass);

        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(recordClass)
                .maxRepeatAttempts(3)
                .build();

        return ChatClient.builder(llmPayloadModel)
                .defaultOptions(OllamaChatOptions.builder()
                        .format(converter.getJsonSchemaMap())
                        .build())
                .defaultAdvisors(validation)
                .build();
    }

}
