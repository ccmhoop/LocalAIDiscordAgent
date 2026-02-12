package com.discord.LocalAIDiscordAgent.chatClient.service;

import com.discord.LocalAIDiscordAgent.tools.webSearch.service.WebSearchMemoryService;
import com.discord.LocalAIDiscordAgent.tools.webSearch.tools.DirectLinkTool;
import com.discord.LocalAIDiscordAgent.tools.webSearch.tools.WebSearchTool;
import com.discord.LocalAIDiscordAgent.chatClient.helpers.ChatClientHelpers;
import com.discord.LocalAIDiscordAgent.chatClient.systemMsg.SystemMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class ToolClientService {

    private final WebSearchMemoryService webSearchMemoryService;
    private final ChatMemory webMemory;
    private final ChatModel chatModel;

    public ToolClientService(
            OllamaChatModel ollamaQwenModelConfig,
            WebSearchMemoryService webSearchMemoryService,
            ChatMemory webMemory
    ) {
        this.webMemory = webMemory;
        this.webSearchMemoryService = webSearchMemoryService;
        this.chatModel = ollamaQwenModelConfig;
    }

    public String generateToolResponse(String userMessage, Map<String, String> metadata, boolean isWebSearch) {

        String conversationId = ChatClientHelpers.buildMetaDataConversationId(metadata);

        try {
            ChatResponse chatResponse = toolChatResponse(metadata.get("username"), userMessage, isWebSearch);

            return ChatClientHelpers.extractOutputTextAsString(chatResponse);

        } catch (Exception e) {
            log.error("Ollama error (conversationId={}): {}",
                    conversationId,
                    e.getMessage(),
                    e
            );
            return "I had a problem generating a response. Please try again.";
        }

    }

    private ChatResponse toolChatResponse(String username, String userMessage, boolean isWebSearch) {

        ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();

        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(handleWebTools(isWebSearch))
                .internalToolExecutionEnabled(false)
                .build();


        SystemMessage system = generateSystemMessage(username, isWebSearch);
        UserMessage userMsg = new UserMessage(userMessage);

        Prompt prompt = new Prompt(List.of(system, userMsg), chatOptions);

        ChatResponse response = chatModel.call(prompt);

        if (response.hasToolCalls()) {
            ToolExecutionResult exec = toolCallingManager.executeToolCalls(prompt, response);
            List<Message> hist = exec.conversationHistory();

            response = chatModel.call(new Prompt(hist));

            if (!webMemory.get(username).isEmpty()) {
                webMemory.clear(username);
            }

            if (!Objects.requireNonNull(response.getResult().getOutput().getText()).isEmpty()) {
                webMemory.add(username, List.of(userMsg, response.getResult().getOutput()));
            }

        }

        return response;
    }

    public boolean shouldUseWebSearch(String userMessage) {
        String t = userMessage.toLowerCase();

        return t.contains("search online")
                || t.contains("websearch")
                || t.contains("google")
                || t.contains("bing")
                || t.contains("source")
                || t.contains("sources")
                || t.contains("lookup")
                || t.contains("look up")
                || t.contains("search");
    }

    public boolean shouldUseDirectLink(String userMessage) {
        String t = userMessage.toLowerCase();

        return t.contains("http://")
                || t.contains("https://")
                || t.contains("www.");
    }

    private SystemMessage generateSystemMessage(String username, boolean isWebSearch) {

        if (!isWebSearch && webMemory.get(username).isEmpty()) {
            return new SystemMessage(SystemMsg.SystemMsgWebTools(LocalDate.now()));
        }

        List<Message> toolMessages = webMemory.get(username);
        Map<MessageType, String> followUpContext = new HashMap<>();

        for (Message message : toolMessages) {
            if (message.getMessageType() == MessageType.USER || message.getMessageType() == MessageType.ASSISTANT) {
                followUpContext.put(message.getMessageType(), message.getText());
            }
        }

        return new SystemMessage(SystemMsg.SystemMsgWebTools(LocalDate.now(), followUpContext));
    }

    private ToolCallback[] handleWebTools(boolean isWebSearch) {
        System.out.println("isWebSearch: " + isWebSearch);

        if (isWebSearch) {
            return ToolCallbacks.from(new WebSearchTool(webSearchMemoryService));
        } else {
            return ToolCallbacks.from(new DirectLinkTool(webSearchMemoryService));
        }
    }

}
