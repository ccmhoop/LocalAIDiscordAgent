package com.discord.LocalAIDiscordAgent.advisor.advisors;

import com.discord.LocalAIDiscordAgent.chatSummary.model.ChatSummary;
import com.discord.LocalAIDiscordAgent.chatMemory.interfaces.ChatMemoryINTF;
import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;
import static org.springframework.ai.chat.messages.MessageType.USER;

public abstract class QwenAdvisor<T extends ChatMemoryINTF>{

    @Getter
    private String augmentedSystemMsg;
    private final String template;
    @Setter
    private String header;
    @Setter
    private String footer;
    @Setter
    @Getter
    public ChatSummary summary;
    @Getter
    private int turnIndex;

    @Getter
    private List<String> turnIds;

    public QwenAdvisor(PromptTemplate template) {
        this.template = template.getTemplate();
    }


    public abstract String chatJsonBody(UserEntity user, String chatMemoryData);
    public abstract String chatJsonTurns(T userMsg, T assistantMsg);
    public abstract String chatJsonMemory();


    public void augmentSystemMsg(Map<MessageType, List<T>> chatMemories, String beforeSystemMsg, UserEntity user) {

        String newTemplate = PromptTemplate.builder()
                .template(body())
                .variables(Map.of(
                        "data", buildChatData(user, chatMemories)
                ))
                .build()
                .render();

        System.out.println(newTemplate);

        this.augmentedSystemMsg = beforeSystemMsg + newTemplate;

    }

    private String body(){
       return header + "\n" + "{data}" + "\n" + footer;
    }

    private String buildChatData(UserEntity user, Map<MessageType, List<T>> chatMemories) {
        List<T> userList = chatMemories.get(USER);
        List<T> assistantList = chatMemories.get(ASSISTANT);

        equalizeListLengths(userList, assistantList);
        Integer size = determineEffectiveSize(userList, assistantList);
        if (size == null ) return "";
        this.turnIndex = userList.size() + assistantList.size();

       List<String> evidenceIds = new ArrayList<>(turnIndex);

        for (int i = 0; i < size; i++) {
            evidenceIds.add(String.valueOf(userList.get(i).getId()));
            evidenceIds.add(String.valueOf(assistantList.get(i).getId()));
        }
        this.turnIds = evidenceIds;

        String chatMemoryData = buildChatMemoryData(size, userList, assistantList);
        return chatJsonBody(user, chatMemoryData);
    }

    private static <T extends ChatMemoryINTF> void equalizeListLengths(List<T> userList, List<T> assistantList) {
        if (userList.size() > assistantList.size()) {
            userList.removeLast();
        } else if (assistantList.size() > userList.size()) {
            assistantList.removeLast();
        }
    }

    private static <T extends ChatMemoryINTF> Integer determineEffectiveSize(List<T> userList, List<T> assistantList) {
        int size;
        if (userList.size() == assistantList.size()) {
            size = userList.size();
        } else {
            if (userList.isEmpty() || assistantList.isEmpty()) {
                userList.clear();
                assistantList.clear();
                return null;
            }
            while (userList.size() > 1) userList.removeLast();
            while (assistantList.size() > 1) assistantList.removeLast();
            size = 1;
        }
        return size;
    }

    private String buildChatMemoryData(Integer size, List<T> userList, List<T> assistantList) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append(
                    chatJsonTurns(
                            userList.get(i),
                            assistantList.get(i)
                    )
            );
        }
        return sb.toString();
    }
}
