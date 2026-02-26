package com.discord.LocalAIDiscordAgent.user.repository;

import com.discord.LocalAIDiscordAgent.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    UserEntity findByUserId(Long userId);
}
