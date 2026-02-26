package com.discord.LocalAIDiscordAgent.chatMemory.webChatMemory.model;

import com.discord.LocalAIDiscordAgent.chatMemory.interfaces.ChatMemoryINTF;
import com.discord.LocalAIDiscordAgent.user.UserEntity;
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
public class WebChatMemory implements ChatMemoryINTF {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;
    @Enumerated(EnumType.STRING)
    private MessageType type;

    private String guildId;
    private String channelId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    private String conversationId;
    private LocalDateTime timestamp;

}
