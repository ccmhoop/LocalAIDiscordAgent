package com.discord.LocalAIDiscordAgent.advisor.advisors;

import com.discord.LocalAIDiscordAgent.chatMemory.interfaces.ChatMemoryINTF;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;
import static org.springframework.ai.chat.messages.MessageType.USER;

public class XmlAdvisor<T extends ChatMemoryINTF>  {

    private final String template;

    @Getter
    private String augmentedSystemMsg;

    public XmlAdvisor(PromptTemplate template) {
        this.template = template.getTemplate();
    }

    public void augmentSystemMsg(Map<MessageType, List<T>> chatMemories, String beforeSystemMsg) {
        String xmlChatContext = buildChatXml(chatMemories);
        this.augmentedSystemMsg = beforeSystemMsg + PromptTemplate.builder()
                .template(template)
                .variables(Map.of(
                        "context", xmlChatContext
                ))
                .build()
                .render();
    }

    private String buildChatXml(Map<MessageType, List<T>> chatMemories) {
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
            sb.append(createMessagePairBlock(
                    userList.get(i).getUsername(),
                    userList.get(i),
                    assistantList.get(i)
            ));
        }

        return sb.toString().stripTrailing();
    }

    private String createMessagePairBlock(String userId, T userChat, T assistantChat) {
        return """
            <message_pair>
            \t<note>
            \t\t<date>%s</date>
            \t\t<to>assistant</to>
            \t\t<from>%s</from>
            \t\t<body>
            %s
            \t\t</body>
            \t</note>
            \t<note>
            \t\t<date>%s</date>
            \t\t<to>%s</to>
            \t\t<from>assistant</from>
            \t\t<body>
            %s
            \t\t</body>
            \t</note>
            </message_pair>
            """
                .formatted(
                        xml(userChat.getTimestamp()),
                        xml(userId),
                        indentBlockXml(userChat.getContent()),
                        xml(assistantChat.getTimestamp()),
                        xml(userId),
                        indentBlockXml(assistantChat.getContent())
                );
    }

    private static String indentBlockXml(String s) {
        if (s == null || s.isBlank()) return "";
        String[] lines = s.split("\\R", -1);
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            if (line.isBlank()) continue;
            out.append("\t\t\t")
                    .append(xml(line.stripLeading()))
                    .append("\n");
        }
        return out.toString().stripTrailing();
    }

    private static String xml(Object o) {
        if (o == null) return "";
        String s = String.valueOf(o);
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

}
