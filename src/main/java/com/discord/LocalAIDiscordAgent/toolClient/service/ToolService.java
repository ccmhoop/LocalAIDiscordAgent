package com.discord.LocalAIDiscordAgent.toolClient.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.UserProfile;
import com.discord.LocalAIDiscordAgent.toolSystemMessage.ToolSystemMsgFactory;
import com.discord.LocalAIDiscordAgent.toolSystemMessage.ToolSystemMsgPresets;
import com.discord.LocalAIDiscordAgent.toolSystemMessage.records.ToolSystemMsgRecords.ToolRuntimeContext;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;
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
    private final ToolSummaryService toolSummaryService;
    private final ToolSystemMsgFactory toolSystemMsgFactory;
    private final WebSearchMemoryService webSearchMemoryService;

    private List<MergedWebQAItem> webQAResults;
    private RecentMessage recentAssistantMsg;
    private UserProfile userProfile;

    public ToolService(
            ChatClient executeToolsClient,
            DiscGlobalData discGlobalData,
            ToolSummaryService toolSummaryService,
            ToolSystemMsgFactory toolSystemMsgFactory,
            WebSearchMemoryService webSearchMemoryService
    ) {
        this.webSearchMemoryService = webSearchMemoryService;
        this.toolSystemMsgFactory = toolSystemMsgFactory;
        this.toolSummaryService = toolSummaryService;
        this.discGlobalData = discGlobalData;
        this.toolClient = executeToolsClient;
    }

    /**
     * Executes tool-related processes using the provided user profile, recent assistant message,
     * and web QA results. This method coordinates various tool execution workflows,
     * including generating prompts, processing tool responses, and summarizing results.
     *
     * @param userProfile        the profile of the current user interacting with the tool
     * @param recentAssistantMsg the most recent message from the assistant
     *                           used in processing tool-related workflows
     * @param webQAResults       a list of merged web QA results providing context or
     *                           additional details for tool execution
     * @return a summarized context generated from the tool outputs,
     * or null if no valid results are available
     */
    public String executeTools(
            UserProfile userProfile,
            RecentMessage recentAssistantMsg,
            List<MergedWebQAItem> webQAResults
    ) {
        this.recentAssistantMsg = recentAssistantMsg;
        this.webQAResults = webQAResults;
        this.userProfile = userProfile;

        String toolsResults = processTools();

        if (toolsResults.isEmpty()) {
            return null;
        }

        String summaryContext = toolSummaryService.summerizeToolResults(toolsResults, recentAssistantMsg);

        if (summaryContext.isEmpty()) {
            return null;
        }

        return summaryContext;
    }

    private String processTools() {
        Prompt prompt = buildToolPrompt();
        ChatResponse toolResponse = runToolLLM(prompt);
        String toolsResults = "";

        if (toolResponse != null && toolResponse.hasToolCalls()){
            toolsResults = handleToolCalls(prompt, toolResponse);
        }

        if (this.webQAResults != null && toolsResults.isEmpty()) {
            toolsResults = webQAResults.toString();
        }

        log.info("sumerizeData: {}", toolsResults);
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
                                new UserMessage(discGlobalData.getUserMessage())
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
                        JsonNode root = objectMapper.readTree(toolContent.responseData());
                        if (root.isTextual()) {
                            root = objectMapper.readTree(root.asText());
                        }
                        StreamSupport.stream(root.path("results").spliterator(), false)
                                .filter(Objects::nonNull)
                                .forEach(contextBuilder::append);

                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON: {}", e.getMessage(), e);
            return "";
        }
        return contextBuilder.toString();
    }

    private ToolRuntimeContext buildToolRuntimeContext() {
        return new ToolRuntimeContext(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                this.userProfile,
                this.webQAResults,
                this.recentAssistantMsg
        );
    }

}