package com.discord.LocalAIDiscordAgent.chatMemory.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecentChatMemory{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String conversationId;
    private String username;
    @Column(columnDefinition = "TEXT")
    private String content;
    private String type;
    private LocalDateTime timestamp;

}

