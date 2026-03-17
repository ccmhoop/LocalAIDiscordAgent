package com.discord.LocalAIDiscordAgent.toolSummary;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.systemMessage.ToolSystemMsgFactory;
import com.discord.LocalAIDiscordAgent.systemMessage.ToolSystemMsgPresets;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.UserProfile;
import com.discord.LocalAIDiscordAgent.systemMessage.records.ToolSystemMsgRecords.ToolRuntimeContext;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;
import com.discord.LocalAIDiscordAgent.webSearch.service.WebSearchMemoryService;
import com.discord.LocalAIDiscordAgent.webSearch.tools.WebSearchTool;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class ToolSummaryService {

    private final ChatModel toolModelLLM;
    private final ChatClient toolClientLLM;
    private final ChatClient toolCallingLLM;
    private final DiscGlobalData discGlobalData;
    private final ToolSystemMsgFactory toolSystemMsgFactory;
    private final WebSearchMemoryService webSearchMemoryService;

    private List<MergedWebQAItem> webQAResults;
    private RecentMessage recentAssistantMsg;
    private UserProfile userProfile;

    public ToolSummaryService(
            ChatModel llmToolConfig,
            ChatClient toolClientLLM,
            ChatClient toolCallingLLM,
            DiscGlobalData discGlobalData,
            ToolSystemMsgFactory toolSystemMsgFactory,
            WebSearchMemoryService webSearchMemoryService
    ) {
        this.webSearchMemoryService = webSearchMemoryService;
        this.toolSystemMsgFactory = toolSystemMsgFactory;
        this.discGlobalData = discGlobalData;
        this.toolCallingLLM = toolCallingLLM;
        this.toolClientLLM = toolClientLLM;
        this.toolModelLLM = llmToolConfig;
    }

    public String processToolResults(
            UserProfile userProfile,
            RecentMessage recentAssistantMsg,
            List<MergedWebQAItem> webQAResults
    ) {
        this.recentAssistantMsg = recentAssistantMsg;
        this.webQAResults = webQAResults;
        this.userProfile = userProfile;

        StringBuilder results = getStringBuilder();
        if (results == null && webQAResults != null) results = new StringBuilder().append(webQAResults);
        if (results == null ) return null;

        log.info("Tool results: {}", results);

        WebSearchToolResult webSearchToolResult = summarizeToolResults(results.toString());

        return webSearchToolResult.summary();
    }

    private WebSearchToolResult summarizeToolResults(String results) {

        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(WebSearchToolResult.class)
                .maxRepeatAttempts(3)
                .build();

        var summaryConv = new BeanOutputConverter<>(WebSearchToolResult.class);

        Map<String, Object> summarySchema = summaryConv.getJsonSchemaMap();

        WebSearchToolResult modelOut = toolClientLLM.prompt()
                .options(OllamaChatOptions.builder()
                        .format(summarySchema)// <- key
                        .internalToolExecutionEnabled(true)
                        .disableThinking()
                        .build())
                .tools(new WebSearchTool(webSearchMemoryService))
                .system("Create a summary using the tool results! Try to summarize based on the user's message and assistant message if available.")
                .user(user -> user.text(
                        userSummaryMSG(results)
                ))
                .advisors(validation)
                .call()
                .entity(WebSearchToolResult.class);

        log.info("Tool summary: {}", modelOut);

        return new WebSearchToolResult(safe(Objects.requireNonNull(modelOut).summary));
    }

    private String userSummaryMSG(String results) {

        StringBuilder sb = new StringBuilder();

        sb.append("tool results: ").append(results).append("\n");

        if (discGlobalData.getUserMessage() != null) {
            sb.append("user Message: ").append(discGlobalData.getUserMessage()).append("\n");
        }

        if (recentAssistantMsg != null) {
            sb.append("assistant Message: ").append(recentAssistantMsg.content()).append("\n");
        }

        return sb.toString();
    }

    @Nullable
    private StringBuilder getStringBuilder() {
        ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();

        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(new WebSearchTool(webSearchMemoryService)))
                .internalToolExecutionEnabled(false)
                .build();

        String toolInstructions = toolSystemMsgFactory.buildToolSystemMsgJson(
                ToolSystemMsgPresets.withContext(buildToolRuntimeContext())
        );

        Prompt prompt = Prompt.builder()
                .messages(
                        List.of(
                                new SystemMessage(toolInstructions),
                                new UserMessage(discGlobalData.getUserMessage())
                        )
                )
                .chatOptions(chatOptions)
                .build();

        log.info("tool prompt : {}", prompt);

        ChatResponse response = toolCallingLLM.prompt(prompt).call().chatResponse();

        log.info("tool response : {}", response);

        List<Message> hist;
        if (response.hasToolCalls()) {
            ToolExecutionResult exec = toolCallingManager.executeToolCalls(prompt, response);
            hist = exec.conversationHistory();
        } else return null;

        StringBuilder results = new StringBuilder();
        for (Message m : hist) {
            if (m instanceof ToolResponseMessage toolResponseMessage) {
                for (ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                    results.append(toolResponse.responseData()).append("\n");
                }
            }
        }
        return results;
    }

    private ToolRuntimeContext buildToolRuntimeContext(){
        return new ToolRuntimeContext(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                this.userProfile,
                this.webQAResults,
                this.recentAssistantMsg
//                discGlobalData.getUserMessage()
        );
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    public record WebSearchToolResult(String summary) {
    }

}
