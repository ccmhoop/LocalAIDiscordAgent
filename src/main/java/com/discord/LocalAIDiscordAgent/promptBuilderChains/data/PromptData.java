package com.discord.LocalAIDiscordAgent.promptBuilderChains.data;

import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;


@Getter
@Component
public class PromptData  {

    private final MapperUtils mapperUtils;

    private String retrievedContext;
    private TestContext testContext;
    private  List<MergedWebQAItem> vectorDBResults;
    private String summary;

    public PromptData(MapperUtils mapperUtils) {
        this.mapperUtils = mapperUtils;
    }

    public String getRetrievedContext() {
        if (testContext == null || testContext.testContext() == null) {
            return null;
        }

        return testContext.testContext();
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSummary() {
        if (summary == null || summary.isBlank()) {
            return null;
        }
        return summary;
    }

    public void setVectorDBResults(List<MergedWebQAItem> vectorDBResults) {
        this.vectorDBResults = vectorDBResults;
    }

    public List<MergedWebQAItem> getVectorDBMemory() {
        return vectorDBResults;
    }

    public void setRetrievedContext(Record contextRecord){
        this.testContext = new TestContext( mapperUtils.valuesToString(contextRecord));
    }

    public void setRetrievedContext(String contextString){
        this.testContext =  new TestContext(contextString);
    }

    public boolean isRetrievedContextPresent() {
        return retrievedContext != null && !retrievedContext.isBlank();
    }

    public record TestContext(String testContext) {}

    public record VectorDBMemory(
            List<MergedWebQAItem> vectorDBResults
    ) {
    }

}
