package com.discord.LocalAIDiscordAgent;

import com.discord.LocalAIDiscordAgent.user.UserEntity;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DataJpaTest
public class TestUniqueUser {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void store_duplicate_unique_userId_test() {

        Long duplicateUserId = 1L;

        var user1 = UserEntity.builder()
                .userId(duplicateUserId)
                .userGlobal("global-1")
                .serverNickname("nick-1")
                .username("user-1")
                .build();

        var user2 = UserEntity.builder()
                .userId(duplicateUserId)
                .userGlobal("global-2")
                .serverNickname("nick-2")
                .username("user-2")
                .build();

        entityManager.persistAndFlush(user1);
        entityManager.clear();

        assertThatThrownBy(() -> entityManager.persistAndFlush(user2))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void givenDifferentUserIds_whenPersisting_thenBothRowsExist() {
        var user1 = UserEntity.builder()
                .userId(1L)
                .userGlobal("global-1")
                .serverNickname("nick-1")
                .username("user-1")
                .build();

        var user2 = UserEntity.builder()
                .userId(2L)
                .userGlobal("global-2")
                .serverNickname("nick-2")
                .username("user-2")
                .build();


        entityManager.persistAndFlush(user1);
        entityManager.persistAndFlush(user2);

        Long count = entityManager.getEntityManager()
                .createQuery("select count(u) from UserEntity u", Long.class)
                .getSingleResult();

        assertThat(count).isEqualTo(2L);
        assertThat(entityManager.find(UserEntity.class, 1L)).isNotNull();
        assertThat(entityManager.find(UserEntity.class, 2L)).isNotNull();
    }
}
