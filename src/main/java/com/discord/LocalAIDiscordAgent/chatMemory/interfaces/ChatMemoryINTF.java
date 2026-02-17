package com.discord.LocalAIDiscordAgent.chatMemory.interfaces;

import com.discord.LocalAIDiscordAgent.webSearch.advisor.WebQuestionAnswerAdvisor.Builder;
import org.springframework.ai.chat.messages.MessageType;

import java.time.LocalDateTime;

public interface ChatMemoryINTF {

    String getConversationId();
    String getUsername();
    String getContent();
    MessageType getType();
    LocalDateTime getTimestamp();
    void setConversationId(String conversationId);
    void setUsername(String username);
    void setContent(String content);
    void setType(MessageType type);
    void setTimestamp(LocalDateTime timestamp);


}
