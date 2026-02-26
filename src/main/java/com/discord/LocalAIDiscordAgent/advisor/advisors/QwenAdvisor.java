package com.discord.LocalAIDiscordAgent.advisor.advisors;

import com.discord.LocalAIDiscordAgent.chatMemory.interfaces.ChatMemoryINTF;
import lombok.Getter;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;
import static org.springframework.ai.chat.messages.MessageType.USER;

public abstract class QwenAdvisor<T extends ChatMemoryINTF>{

    @Getter
    private String augmentedSystemMsg;
    private final String template;

    public QwenAdvisor(PromptTemplate template) {
        this.template = template.getTemplate();
    }

    public abstract String chatMemoryBody(T user, String chatMemoryData);
    public abstract String chatMemoryData(T userMsg, T assistantMsg);


    public void augmentSystemMsg(Map<MessageType, List<T>> chatMemories, String beforeSystemMsg) {
        this.augmentedSystemMsg = beforeSystemMsg + PromptTemplate.builder()
                .template(template)
                .variables(Map.of(
                        "data", buildChatData(chatMemories)
                ))
                .build()
                .render();

        System.out.println(augmentedSystemMsg);
    }

    private String buildChatData(Map<MessageType, List<T>> chatMemories) {
        List<T> userList = chatMemories.get(USER);
        List<T> assistantList = chatMemories.get(ASSISTANT);

        if (userList.size() > assistantList.size()) {
            userList.removeLast();
        } else if (assistantList.size() > userList.size()) {
            assistantList.removeLast();
        }

        int size;
        if (userList.size() == assistantList.size()) {
            size = userList.size();
        } else {
            if (userList.isEmpty() || assistantList.isEmpty()) {
                userList.clear();
                assistantList.clear();
                return "";
            }

            while (userList.size() > 1) userList.removeLast();
            while (assistantList.size() > 1) assistantList.removeLast();
            size = 1;
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < size; i++) {
            sb.append(
                    chatMemoryData(
                            userList.get(i),
                            assistantList.get(i)
                    )
            );
        }

        return chatMemoryBody(userList.getFirst(), sb.toString());
    }
}
