package com.discord.LocalAIDiscordAgent.webSearch.advisor;

import com.discord.LocalAIDiscordAgent.advisor.templates.AdvisorTemplates;
import com.discord.LocalAIDiscordAgent.webSearch.helpers.WebSearchChunkMerger;
import com.discord.LocalAIDiscordAgent.webSearch.service.WebSearchMemoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.*;

@Getter
public class WebQuestionAnswerAdvisor implements BaseAdvisor {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String TIER_WEB_SEARCH = "WEB_SEARCH";
    private static final String WEB_SEARCH_FILTER_EXPRESSION = "tier == '" + TIER_WEB_SEARCH + "'";

    public static final String RETRIEVED_DOCUMENTS = "qa_retrieved_documents";
    public static final String FILTER_EXPRESSION = "qa_filter_expression";
    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("{query}\n\nContext information is below, surrounded by ---------------------\n\n---------------------\n{question_answer_context}\n---------------------\n\nGiven the context and provided history information and not prior knowledge,\nreply to the user comment. If the answer is not in the context, inform\nthe user that you can't answer the question.\n");
    private static final int DEFAULT_ORDER = 0;
    private final WebSearchMemoryService service;
    private final VectorStore vectorStore;
    private final PromptTemplate promptTemplate;
    private final SearchRequest searchRequest;
    private final Scheduler scheduler;
    private final int order;

    WebQuestionAnswerAdvisor(WebSearchMemoryService webSearchMemoryService, VectorStore vectorStore, SearchRequest searchRequest, @Nullable PromptTemplate promptTemplate, @Nullable Scheduler scheduler, int order) {
        this.service = webSearchMemoryService;
        Assert.notNull(vectorStore, "vectorStore cannot be null");
        Assert.notNull(searchRequest, "searchRequest cannot be null");
        this.vectorStore = vectorStore;
        this.searchRequest = searchRequest;
        this.promptTemplate = promptTemplate != null ? promptTemplate : DEFAULT_PROMPT_TEMPLATE;
        this.scheduler = scheduler != null ? scheduler : BaseAdvisor.DEFAULT_SCHEDULER;
        this.order = order;
    }

    public static Builder builder(WebSearchMemoryService webSearchMemoryService, VectorStore vectorStore ) {
        return new Builder(webSearchMemoryService, vectorStore);
    }

    @Override
    @NonNull
    public ChatClientRequest before(ChatClientRequest chatClientRequest, @NonNull AdvisorChain advisorChain) {
        SearchRequest searchRequestToUse = SearchRequest.from(this.searchRequest).query(chatClientRequest.prompt().getUserMessage().getText()).filterExpression(this.doGetFilterExpression(chatClientRequest.context())).build();
        String userMessage = chatClientRequest.prompt().getUserMessage().getText();
        SystemMessage previousSystemMsg = chatClientRequest.prompt().getSystemMessage();
        String newSystemMessage = previousSystemMsg.getText() + buildPromptTemplate(userMessage);
        return chatClientRequest.mutate().prompt(chatClientRequest.prompt().augmentSystemMessage(newSystemMessage)).build();
    }

    @Override
    @NonNull
    public ChatClientResponse after(@NonNull ChatClientResponse chatClientResponse, @NonNull AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Nullable
    protected Filter.Expression doGetFilterExpression(Map<String, Object> context) {
        return context.containsKey("qa_filter_expression") && StringUtils.hasText(context.get("qa_filter_expression").toString()) ? (new FilterExpressionTextParser()).parse(context.get("qa_filter_expression").toString()) : this.searchRequest.getFilterExpression();
    }

    private String buildPromptTemplate(String query) {

        WebSearchChunkMerger.MergedWebResults merged = service.searchExistingContent(query);

        StringBuilder sb = new StringBuilder();

        if (merged == null || merged.count() == 0) {return ""; }

        for (var item : merged.results()) {
            sb.append(createNotes(item.rank(),item.content()));
        }

        return PromptTemplate.builder()
                .template(AdvisorTemplates.WEB_SEARCH_QUESTION_ANSWER.getTemplate())
                .variables(Map.of(
                        "web_notes", sb.toString().stripTrailing()
                ))
                .build()
                .render();
    }

    private String createNotes(int rank, String content) {
        return """
                <note>
                \t<rank>%d</rank>
                \t<body>
                %s
                \t</body>
                /<note>
                """
                .formatted(rank,indentBlock(content));
    }

    private static String indentBlock(String s) {
        if (s == null || s.isBlank()) return "";
        String[] lines = s.split("\\R", -1);
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            if (line.isBlank()) continue;
            out.append("\t\t").append(line.stripLeading()).append("\n");
        }
        return out.toString().stripTrailing();
    }

    public static final class Builder {
        private final VectorStore vectorStore;
        private SearchRequest searchRequest = SearchRequest.builder().build();
        private PromptTemplate promptTemplate;
        private Scheduler scheduler;
        private int order = 0;
        private final WebSearchMemoryService webSearchMemoryService;

        private Builder( WebSearchMemoryService webSearchMemoryService, VectorStore vectorStore) {
            this.webSearchMemoryService = webSearchMemoryService;
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
            return new WebQuestionAnswerAdvisor(this.webSearchMemoryService, this.vectorStore, this.searchRequest, this.promptTemplate, this.scheduler, this.order);
        }
    }

}
