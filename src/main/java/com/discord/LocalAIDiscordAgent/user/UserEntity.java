package com.discord.LocalAIDiscordAgent.user;


import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @Column(unique=true)
    private Long userId;
    private String userGlobal;
    private String serverNickname;
    private String username;

}
