package com.discord.LocalAIDiscordAgent.toolClient.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.toolSystemMessage.ToolSystemMsgFactory;
import com.discord.LocalAIDiscordAgent.toolSystemMessage.ToolSystemMsgPresets;
import com.discord.LocalAIDiscordAgent.toolSystemMessage.records.ToolSystemMsgRecords.ToolRuntimeContext;
import com.discord.LocalAIDiscordAgent.webSearch.service.WebSearchMemoryService;
import com.discord.LocalAIDiscordAgent.webSearch.tools.WebSearchTool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class ToolService {

    private final ChatClient toolClient;
    private final DiscGlobalData discGlobalData;
//    private final ToolSummaryService toolSummaryService;
    private final ToolSystemMsgFactory toolSystemMsgFactory;
    private final WebSearchMemoryService webSearchMemoryService;

//    private List<MergedWebQAItem> webQAResults;
    private RecentMessage recentAssistantMsg;

    public ToolService(
            ChatClient executeToolsClient,
            DiscGlobalData discGlobalData,
            ToolSystemMsgFactory toolSystemMsgFactory,
            WebSearchMemoryService webSearchMemoryService
    ) {
        this.webSearchMemoryService = webSearchMemoryService;
        this.toolSystemMsgFactory = toolSystemMsgFactory;
//        this.toolSummaryService = toolSummaryService;
        this.discGlobalData = discGlobalData;
        this.toolClient = executeToolsClient;
    }

    /**
     * Executes tool-related processes using the provided user profile, recent assistant message,
     * and web QA results. This method coordinates various tool execution workflows,
     * including generating prompts, processing tool responses, and summarizing results.
     *
     *                           used in processing tool-related workflows
     *                           additional details for tool execution
     * @return a summarized context generated from the tool outputs,
     * or null if no valid results are available
     */
    public String executeTools() {
//        this.webQAResults = webQAResults;

        String toolsResults = processTools();

        if (toolsResults.isEmpty()) {
            return null;
        }

        return toolsResults;
    }

    private String processTools() {
        Prompt prompt = buildToolPrompt();
        ChatResponse toolResponse = runToolLLM(prompt);
        String toolsResults = "";

        if (toolResponse != null && toolResponse.hasToolCalls()){
            toolsResults = handleToolCalls(prompt, toolResponse);
            log.debug("web_search results: {}", toolsResults);
        }

        return toolsResults;
    }

    private Prompt buildToolPrompt() {
        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(new WebSearchTool(webSearchMemoryService)))
                .internalToolExecutionEnabled(false)
                .build();

        String toolInstructions = toolSystemMsgFactory.buildToolSystemMsgJson(
                ToolSystemMsgPresets.withContext(buildToolRuntimeContext())
        );

        return Prompt.builder()
                .messages(
                        List.of(
                                new SystemMessage(toolInstructions),
                                //place holder
                                new UserMessage("Current_date : "+ LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)+ "\n user_message: " +discGlobalData.getUserMessage())
                        )
                )
                .chatOptions(chatOptions)
                .build();
    }

    private ChatResponse runToolLLM(Prompt prompt) {
        return toolClient.prompt(prompt).call().chatResponse();
    }

    private String handleToolCalls(Prompt prompt, ChatResponse toolResponse){
        StringBuilder contextBuilder = new StringBuilder();
        ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();
        ToolExecutionResult exec = toolCallingManager.executeToolCalls(prompt, toolResponse);
        List<Message> hist = exec.conversationHistory();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            for (Message m : hist) {
                if (m instanceof ToolResponseMessage toolResponseMessage) {
                    for (ToolResponse toolContent : toolResponseMessage.getResponses()) {
                        String responseData = toolContent.responseData();

                        if (responseData.trim().isEmpty()) {
                            log.warn("Tool response data is null or empty for tool: {}", toolContent.name());
                            continue;
                        }

                        log.debug("Tool response data length: {} chars", responseData.length());
                        if (responseData.length() > 4000) {
                            log.debug("Response data sample around position 4000: {}",
                                    responseData.substring(Math.max(0, 3950), Math.min(responseData.length(), 4050)));
                        }

                        try {
                            JsonNode root = objectMapper.readTree(responseData);

                            if (root.isTextual()) {
                                String textContent = root.asText();
                                if (isValidJson(textContent)) {
                                    root = objectMapper.readTree(textContent);
                                } else {
                                    log.warn("Tool response contains non-JSON text content: {}",
                                            textContent.length() > 100 ? textContent.substring(0, 100) + "..." : textContent);
                                    contextBuilder.append(textContent).append("\n");
                                    continue;
                                }
                            }

                            JsonNode resultsNode = root.path("results");
                            if (resultsNode.isMissingNode()) {
                                log.warn("No 'results' field found in tool response");
                                contextBuilder.append(root).append("\n");
                            } else {
                                StreamSupport.stream(resultsNode.spliterator(), false)
                                        .filter(Objects::nonNull)
                                        .forEach(result -> contextBuilder.append(result).append("\n"));
                            }

                        } catch (JsonProcessingException jsonEx) {
                            log.error("Failed to parse tool response as JSON for tool: {}. Error: {}. " +
                                            "Response length: {} chars. Response preview: {}",
                                    toolContent.name(),
                                    jsonEx.getMessage(),
                                    responseData.length(),
                                    responseData.length() > 200 ? responseData.substring(0, 200) + "..." : responseData);

                            contextBuilder.append(responseData).append("\n");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error processing tool calls: {}", e.getMessage(), e);
            return "";
        }

        return contextBuilder.toString();
    }

    private boolean isValidJson(String jsonString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(jsonString);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
    private ToolRuntimeContext buildToolRuntimeContext() {
        return new ToolRuntimeContext(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                discGlobalData.getUserProfile(),
//                this.webQAResults,
                null,
                this.recentAssistantMsg
        );
    }

}