package com.discord.LocalAIDiscordAgent.Advisor.advisor;

import lombok.Getter;
import lombok.NonNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class WebQuestionAnswerAdvisor implements BaseAdvisor {

    public static final String RETRIEVED_DOCUMENTS = "qa_retrieved_documents";
    public static final String FILTER_EXPRESSION = "qa_filter_expression";
    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("{query}\n\nContext information is below, surrounded by ---------------------\n\n---------------------\n{question_answer_context}\n---------------------\n\nGiven the context and provided history information and not prior knowledge,\nreply to the user comment. If the answer is not in the context, inform\nthe user that you can't answer the question.\n");
    private static final int DEFAULT_ORDER = 0;
    private final VectorStore vectorStore;
    private final PromptTemplate promptTemplate;
    private final SearchRequest searchRequest;
    private final Scheduler scheduler;
    private final int order;

    WebQuestionAnswerAdvisor(VectorStore vectorStore, SearchRequest searchRequest, @Nullable PromptTemplate promptTemplate, @Nullable Scheduler scheduler, int order) {
        Assert.notNull(vectorStore, "vectorStore cannot be null");
        Assert.notNull(searchRequest, "searchRequest cannot be null");
        this.vectorStore = vectorStore;
        this.searchRequest = searchRequest;
        this.promptTemplate = promptTemplate != null ? promptTemplate : DEFAULT_PROMPT_TEMPLATE;
        this.scheduler = scheduler != null ? scheduler : BaseAdvisor.DEFAULT_SCHEDULER;
        this.order = order;
    }

    public static Builder builder(VectorStore vectorStore) {
        return new Builder(vectorStore);
    }

    @Override
    @NonNull
    public ChatClientRequest before(ChatClientRequest chatClientRequest, @NonNull AdvisorChain advisorChain) {
        SearchRequest searchRequestToUse = SearchRequest.from(this.searchRequest).query(chatClientRequest.prompt().getUserMessage().getText()).filterExpression(this.doGetFilterExpression(chatClientRequest.context())).build();
        List<Document> documents = this.vectorStore.similaritySearch(searchRequestToUse);
        Map<String, Object> context = new HashMap<>(chatClientRequest.context());
        context.put("qa_retrieved_documents", documents);
        String documentContext = (String)documents.stream().map(Document::getText).collect(Collectors.joining(System.lineSeparator()));
        UserMessage userMessage = chatClientRequest.prompt().getUserMessage();
        String augmentedUserText = this.promptTemplate.render(Map.of("query", userMessage.getText(), "question_answer_context", documentContext));
        return chatClientRequest.mutate().prompt(chatClientRequest.prompt().augmentUserMessage(augmentedUserText)).context(context).build();
    }

    @Override
    @NonNull
    public ChatClientResponse after(ChatClientResponse chatClientResponse, @NonNull AdvisorChain advisorChain) {
        ChatResponse.Builder chatResponseBuilder;
        if (chatClientResponse.chatResponse() == null) {
            chatResponseBuilder = ChatResponse.builder();
        } else {
            chatResponseBuilder = ChatResponse.builder().from(chatClientResponse.chatResponse());
        }

        chatResponseBuilder.metadata("qa_retrieved_documents", chatClientResponse.context().get("qa_retrieved_documents"));
        return ChatClientResponse.builder().chatResponse(chatResponseBuilder.build()).context(chatClientResponse.context()).build();
    }

    @Nullable
    protected Filter.Expression doGetFilterExpression(Map<String, Object> context) {
        return context.containsKey("qa_filter_expression") && StringUtils.hasText(context.get("qa_filter_expression").toString()) ? (new FilterExpressionTextParser()).parse(context.get("qa_filter_expression").toString()) : this.searchRequest.getFilterExpression();
    }


    public static final class Builder {
        private final VectorStore vectorStore;
        private SearchRequest searchRequest = SearchRequest.builder().build();
        private PromptTemplate promptTemplate;
        private Scheduler scheduler;
        private int order = 0;

        private Builder(VectorStore vectorStore) {
            Assert.notNull(vectorStore, "The vectorStore must not be null!");
            this.vectorStore = vectorStore;
        }

        public Builder promptTemplate(PromptTemplate promptTemplate) {
            Assert.notNull(promptTemplate, "promptTemplate cannot be null");
            this.promptTemplate = promptTemplate;
            return this;
        }

        public Builder searchRequest(SearchRequest searchRequest) {
            Assert.notNull(searchRequest, "The searchRequest must not be null!");
            this.searchRequest = searchRequest;
            return this;
        }

        public Builder protectFromBlocking(boolean protectFromBlocking) {
            this.scheduler = protectFromBlocking ? BaseAdvisor.DEFAULT_SCHEDULER : Schedulers.immediate();
            return this;
        }

        public Builder scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public WebQuestionAnswerAdvisor build() {
            return new WebQuestionAnswerAdvisor(this.vectorStore, this.searchRequest, this.promptTemplate, this.scheduler, this.order);
        }
    }

}
