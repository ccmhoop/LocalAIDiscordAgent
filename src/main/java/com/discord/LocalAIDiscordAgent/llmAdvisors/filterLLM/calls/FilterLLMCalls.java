package com.discord.LocalAIDiscordAgent.llmAdvisors.filterLLM.calls;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmAdvisors.filterLLM.llm.FilterLLM;
import com.discord.LocalAIDiscordAgent.llmMemory.advisorRequests.filterLLM.ReduceChatMemoryRequest;
import com.discord.LocalAIDiscordAgent.llmMemory.chatMemory.recentChatMemory.service.RecentChatMemoryService;
import com.discord.LocalAIDiscordAgent.llmMemory.records.ChatMemoryPayload;
import com.discord.LocalAIDiscordAgent.llmMemory.records.FilteredChatMemoryOutput;
import com.discord.LocalAIDiscordAgent.systemMessage.records.SystemMsgRecords.RecentMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public final class FilterLLMCalls {

    private final FilterLLM llm;
    private final DiscGlobalData discGlobalData;

    public FilterLLMCalls(FilterLLM filterLLM, DiscGlobalData discGlobalData) {
        this.llm = filterLLM;
        this.discGlobalData = discGlobalData;
    }

    public ChatMemoryPayload removeUnnecessaryChatMemory(){
        Record record = llm.call(new ReduceChatMemoryRequest(discGlobalData));
        log.info("Reduced chat memory: {}", record);

        if (record instanceof FilteredChatMemoryOutput(boolean includeLongTermMemory, List<Integer> ids)) {

            List<RecentMessage> recentMessages = RecentChatMemoryService.reduceAndBuildRecentMessages(
                    discGlobalData.getAssistantMessages(),
                    discGlobalData.getUserMessages(),
                    ids
            );

            return new ChatMemoryPayload(
                    includeLongTermMemory ? discGlobalData.getLongTermMemoryData(): null,
                    null,
                    recentMessages.isEmpty()? null : recentMessages
            );
        }
        return null;
    }


}
