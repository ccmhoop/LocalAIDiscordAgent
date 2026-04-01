package com.discord.LocalAIDiscordAgent.llmMemory.advisorRequests.filterLLM;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.llmAdvisors.filterLLM.records.FilterLLMContextRecord;
import com.discord.LocalAIDiscordAgent.llmAdvisors.filterLLM.records.FilterLLMContextRecord.ChatMessagesRecord;
import com.discord.LocalAIDiscordAgent.llmAdvisors.filterLLM.records.FilterLLMContextRecord.ChatMessagesRecord.MessagePair;
import com.discord.LocalAIDiscordAgent.llmAdvisors.filterLLM.records.FilterLLMContextRecord.ChatMessagesRecord.MessagePair.ChatMessage;
import com.discord.LocalAIDiscordAgent.llmAdvisors.filterLLM.request.FilterRequest;
import com.discord.LocalAIDiscordAgent.llmMemory.chatMemory.recentChatMemory.model.RecentChatMemory;
import com.discord.LocalAIDiscordAgent.llmMemory.records.FilteredChatMemoryOutput;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class ReduceChatMemoryRequest extends FilterRequest {

    private static final String SYSTEM_MESSAGE = """
            You are a strict chat memory extraction assistant.
            
            Your task is to analyze the <memory> and extract ids of valuable messages for contextualization based on the user_message :
            - ids: the message IDs to include in the output
            - includeLongTermMemory: whether long-term memory should be included
            
            Rules:
            1. Extract only explicit the message IDs from the <memory>.
            2. Add the id to the List<Integer> ids.
            4. Do not infer, guess, or invent missing IDs.
            5. Decide if includeLongTermMemory if valuable for contextualization.
            
            <memory>
            %s
            </memory>
            """;

    public ReduceChatMemoryRequest(DiscGlobalData discGlobalData) {
        super(
                FilteredChatMemoryOutput.class,
                SYSTEM_MESSAGE,
                new FilterLLMContextRecord(
                        discGlobalData.getLongTermMemoryData(),
                        buildRecord(discGlobalData)
                )
        );
    }

    private static ChatMessagesRecord buildRecord(DiscGlobalData discGlobalData) {
        int size = discGlobalData.getAssistantMessages().size();
        List<RecentChatMemory> userMessages = discGlobalData.getUserMessages();
        List<RecentChatMemory> assistantMessages = discGlobalData.getAssistantMessages();
        List<MessagePair> pairs = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            RecentChatMemory user = userMessages.get(i);
            RecentChatMemory assistant = assistantMessages.get(i);
            MessagePair pair = null;
            if (user.getTimestamp().toString().equals(assistant.getTimestamp().toString())) {
                pair = new MessagePair(
                        i,
                        user.getTimestamp().truncatedTo(ChronoUnit.SECONDS).toString(),
                        List.of(
                                new ChatMessage(user.getContent(), assistant.getContent())
                        ),
                        null
                );
            }else {
                if (user.getContent() != null) {
                    pair = handleSingle(user ,user.getContent(), null , i);
                }

                if (assistant.getContent() != null) {
                    pair = handleSingle(assistant,null, assistant.getContent(), i);
                }
            }

            if (pair != null) {
                pairs.add(pair);
            }
        }

        return new ChatMessagesRecord(pairs);
    }

    private static MessagePair handleSingle(RecentChatMemory chatMemory, String userContent, String assistantContent, int i) {
        return new MessagePair(
                i,
                chatMemory.getTimestamp().truncatedTo(ChronoUnit.SECONDS).toString(),
                null,
                new ChatMessage(
                        userContent,
                        assistantContent
                )
        );
    }

}
