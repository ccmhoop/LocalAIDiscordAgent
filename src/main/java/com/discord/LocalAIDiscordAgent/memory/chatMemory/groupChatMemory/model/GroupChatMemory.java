package com.discord.LocalAIDiscordAgent.memory.chatMemory.groupChatMemory.model;

import com.discord.LocalAIDiscordAgent.memory.chatMemory.interfaces.ChatMemoryINTF;
import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.ai.chat.messages.MessageType;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupChatMemory implements ChatMemoryINTF {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String conversationId;
    @Column(columnDefinition = "TEXT")
    private String content;
    @Enumerated(EnumType.STRING)
    private MessageType type;
    private String guildId;
    private String channelId;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(precision = 0)
    private LocalDateTime timestamp;

}
