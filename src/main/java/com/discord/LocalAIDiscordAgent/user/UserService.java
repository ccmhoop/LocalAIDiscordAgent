package com.discord.LocalAIDiscordAgent.user;

import com.discord.LocalAIDiscordAgent.discord.enums.DiscDataKey;
import com.discord.LocalAIDiscordAgent.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class UserService {

    public final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public UserEntity getUser(Map<DiscDataKey, String> discDataMap) {
        Long userId = Long.parseLong(discDataMap.get(DiscDataKey.USER_ID));
        return userRepository.findByUserId(userId);
    }

    public void saveUser (UserEntity userEntity) {
        try {
            userRepository.saveAndFlush(userEntity);
        }catch (ConstraintViolationException e) {
            log.error("Error saving user: {}", e.getMessage(), e);
        }
    }

    public UserEntity buildUser(Map<DiscDataKey, String> discDataMap) {
        return UserEntity.builder()
                .userId(Long.parseLong(discDataMap.get(DiscDataKey.USER_ID)))
                .userGlobal(discDataMap.get(DiscDataKey.USER_GLOBAL))
                .serverNickname(discDataMap.get(DiscDataKey.SERVER_NICKNAME))
                .username(discDataMap.get(DiscDataKey.USERNAME))
                .build();

    }

}
