package com.discord.LocalAIDiscordAgent.advisor.advisors;

import com.discord.LocalAIDiscordAgent.chatMemory.interfaces.ChatMemoryINTF;
import lombok.Getter;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;
import static org.springframework.ai.chat.messages.MessageType.USER;

public class QwenAdvisor<T extends ChatMemoryINTF> extends AbstractQwenAdvisor {

    @Getter
    private String augmentedSystemMsg;
    private final String template;

    public QwenAdvisor(PromptTemplate template) {
        this.template = template.getTemplate();
    }

    public void augmentSystemMsg(Map<MessageType, List<T>> chatMemories, String beforeSystemMsg) {
        this.augmentedSystemMsg = beforeSystemMsg + PromptTemplate.builder()
                .template(template)
                .variables(Map.of(
                        "data", buildChatData(chatMemories)
                ))
                .build()
                .render();
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
            sb.append(createDataBlock(
                    userList.get(i).getUsername(),
                    userList.get(i),
                    assistantList.get(i)
            )).append(i == size - 1 ? "" : "\n");
        }

        return sb.substring(0, sb.length() - 1).stripTrailing();
    }

    private String createDataBlock(String userId, T userChat, T assistantChat) {
        return """
                {
                    date: "%s",
                    user.name: "%s",
                    user.sent: "%s",
                    assistant.respond: respond: "%s",
                },
                """.formatted(
                indentBlock(userChat.getTimestamp().toString()),
                indentBlock(userId),
                indentBlock(userChat.getContent()),
                indentBlock(assistantChat.getContent())
        ).stripTrailing();
    }
}
