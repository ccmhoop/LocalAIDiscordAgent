package com.discord.LocalAIDiscordAgent.chatClient.service;

import com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey;
import com.discord.LocalAIDiscordAgent.interactionProcessor.ProcessToolClient;
import com.discord.LocalAIDiscordAgent.user.UserEntity;
import com.discord.LocalAIDiscordAgent.webSearch.service.WebSearchMemoryService;
import com.discord.LocalAIDiscordAgent.webSearch.tools.DirectLinkTool;
import com.discord.LocalAIDiscordAgent.webSearch.tools.WebSearchTool;
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
    private final ChatModel chatModel;
    private final ProcessToolClient process;


    public ToolClientService(
            OllamaChatModel ollamaQwenModelConfig,
            WebSearchMemoryService webSearchMemoryService,
            ProcessToolClient process
    ) {
        this.webSearchMemoryService = webSearchMemoryService;
        this.chatModel = ollamaQwenModelConfig;
        this.process = process;
    }

    public String generateToolResponse(String userMessage, Map<DiscDataKey, String> discDataMap, UserEntity user, boolean isWebSearch) {

        String conversationId = ChatClientHelpers.buildConversationId(discDataMap);

        try {
            ChatResponse chatResponse = toolChatResponse(discDataMap.get(DiscDataKey.USERNAME), userMessage, isWebSearch);
            String assistantMessage = ChatClientHelpers.extractOutputTextAsString(chatResponse);

            try {
                List<Message> messages = List.of(new UserMessage(userMessage), new AssistantMessage(assistantMessage));
                process.saveInteraction(discDataMap, messages, user);
                log.debug("Successfully saved chat interaction for user: {}", discDataMap.get(DiscDataKey.USER_ID));
            }catch (Exception saveException) {
                log.error("Failed to save chat memory for user: {} - Error: {}",
                    discDataMap.get(DiscDataKey.USER_ID), saveException.getMessage(), saveException);
            }

            return assistantMessage;

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
        return new SystemMessage(SystemMsg.SystemMsgWebTools(LocalDate.now()));

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
