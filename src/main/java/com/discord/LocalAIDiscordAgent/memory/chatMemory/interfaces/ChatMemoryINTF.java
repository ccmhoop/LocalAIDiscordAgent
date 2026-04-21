package com.discord.LocalAIDiscordAgent.memory.chatMemory.interfaces;

import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
import org.springframework.ai.chat.messages.MessageType;

import java.time.LocalDateTime;

public interface ChatMemoryINTF {

    Long getId();
    String getConversationId();
    String getContent();
    MessageType getType();
    LocalDateTime getTimestamp();
    void setConversationId(String conversationId);
    void setContent(String content);
    void setType(MessageType type);
    void setTimestamp(LocalDateTime timestamp);
    String getGuildId();
    String getChannelId();

    UserEntity getUser();
    void setUser(UserEntity user);


}
