package com.discord.LocalAIDiscordAgent.chatMemory.toolChatMemory.advisor;

import com.discord.LocalAIDiscordAgent.advisor.templates.AdvisorTemplates;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.util.Assert;
import reactor.core.scheduler.Scheduler;

import java.util.List;
import java.util.Map;

@Getter
public class WebMemoryAdvisor implements BaseChatMemoryAdvisor {

    private final ChatMemory webMemory;
    private final String defaultConversationId;
    private final int order;
    private final Scheduler scheduler;

    private WebMemoryAdvisor(ChatMemory webMemory, String defaultConversationId, int order, Scheduler scheduler) {
        Assert.notNull(webMemory, "webMemory cannot be null");
        Assert.hasText(defaultConversationId, "defaultConversationId cannot be null or empty");
        Assert.notNull(scheduler, "scheduler cannot be null");
        this.webMemory = webMemory;
        this.defaultConversationId = defaultConversationId;
        this.order = order;
        this.scheduler = scheduler;
    }

    @Override
    @NonNull
    public ChatClientRequest before(ChatClientRequest chatClientRequest, @NonNull AdvisorChain advisorChain) {
        String conversationId = this.getConversationId(chatClientRequest.context(), this.defaultConversationId);
        List<Message> webResultMessages = this.webMemory.get(conversationId);
        SystemMessage processedMessages = chatClientRequest.prompt().getSystemMessage();
        String systemMessage = "</SystemMessage>";

        if (!webResultMessages.isEmpty()) {
            String augmentedInstructionText = PromptTemplate.builder()
                    .template(AdvisorTemplates.WEB_SEARCH_MEMORY_TEMPLATE.getTemplate()) // or .template(AdvisorTemplates.WEB_SEARCH_MEMORY_TEMPLATE)
                    .variables(Map.of(
                            "web_search_memory", buildWebSearchMemory(webResultMessages)
                    ))
                    .build()
                    .render();

            systemMessage = processedMessages.getText() + augmentedInstructionText;
        }else{
            systemMessage =  processedMessages.getText() + systemMessage;
        }

        ChatClientRequest processedChatClientRequest = chatClientRequest.mutate().prompt(chatClientRequest.prompt().augmentSystemMessage(systemMessage)).build();

        System.out.println(processedChatClientRequest);

        return processedChatClientRequest;

    }

    @Override
    @NonNull
    public ChatClientResponse after(@NonNull ChatClientResponse chatClientResponse, @NonNull AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    private static String buildWebSearchMemory(List<Message> webResultMessages) {
        if (webResultMessages == null || webResultMessages.isEmpty()) {
            return "None";
        }

        Message first = webResultMessages.getFirst();
        Message last = webResultMessages.getLast();

        String userQ = first != null ? safe(first.getText()) : "";
        String assistantA = last != null ? safe(last.getText()) : "";

        return """
                <message_pair>
                \t<user_question>
                %s
                \t</user_question>
                \t<assistant_response>
                %s
                \t</assistant_response>
                </message_pair>
                """.formatted(indentBlock(userQ, "\t\t"), indentBlock(assistantA, "\t\t")).trim();
    }

    private static String indentBlock(String s, String indent) {
        if (s == null || s.isBlank()) return "";
        String[] lines = s.split("\\R", -1);
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            if (line.isBlank()) continue;
            out.append(indent).append(line.stripLeading()).append('\n');
        }
        return out.toString().stripTrailing();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }


    public static Builder builder(ChatMemory webMemory) {
        return new Builder(webMemory);
    }


    public static final class Builder {
        private String conversationId = "default";
        private int order = -2147482648;
        private Scheduler scheduler;
        private final ChatMemory webMemory;

        private Builder(ChatMemory webMemory) {
            this.scheduler = BaseAdvisor.DEFAULT_SCHEDULER;
            this.webMemory = webMemory;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public WebMemoryAdvisor build() {
            return new WebMemoryAdvisor(this.webMemory, this.conversationId, this.order, this.scheduler);
        }
    }

}
