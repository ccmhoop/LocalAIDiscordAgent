package com.discord.LocalAIDiscordAgent.aiSystemMsgBuilder.service;

import com.discord.LocalAIDiscordAgent.aiSystemMsgBuilder.systemMsg.AISystemMsg;
import com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.service.AiMemoryContextService;
import com.discord.LocalAIDiscordAgent.aiTools.aiWebSearch.systemMsg.ToolSystemMsg;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SystemMsgService {

    private final AiMemoryContextService memoryContextService;

    public SystemMsgService(AiMemoryContextService memoryContextBuilderService) {
        this.memoryContextService = memoryContextBuilderService;
    }

    public List<Message> systemMsg (String userId, String userMessage){

        List<Message> messages = new ArrayList<>();

        messages.add(new SystemMessage(AISystemMsg.SYSTEM_MESSAGE_SCOTTISH_AGENT));

        memoryContextService.buildMessages(messages, userId, userMessage );

        return  messages;
    }

}
