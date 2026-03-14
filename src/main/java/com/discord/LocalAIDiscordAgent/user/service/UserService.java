package com.discord.LocalAIDiscordAgent.user.service;

import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey;
import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
import com.discord.LocalAIDiscordAgent.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class UserService {

    public final UserRepository userRepository;
    public final DiscGlobalData discGlobalData;

    public UserService(UserRepository userRepository, DiscGlobalData discGlobalData) {
        this.userRepository = userRepository;
        this.discGlobalData = discGlobalData;
    }

    public UserEntity getUser() {
        Long userId = Long.parseLong(discGlobalData.getUserId());
        return userRepository.findByUserId(userId);
    }

    public void updateUser(UserEntity userEntity , String nickname) {
        userEntity.setServerNickname(nickname);
        userRepository.saveAndFlush(userEntity);
    }

    public void saveUser (UserEntity userEntity) {
        try {
            userRepository.saveAndFlush(userEntity);
        }catch (ConstraintViolationException e) {
            log.error("Error saving user: {}", e.getMessage(), e);
        }
    }

    public UserEntity buildUser() {
        return UserEntity.builder()
                .userId(Long.parseLong(discGlobalData.getUserId()))
                .userGlobal(discGlobalData.getUserGlobal())
                .serverNickname(discGlobalData.getServerNickname())
                .username(discGlobalData.getUsername())
                .build();

    }

}
