package com.discord.LocalAIDiscordAgent.promptBuilderChains.data;

import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import lombok.Getter;
import org.springframework.stereotype.Component;


@Getter
@Component
public class PromptData  {

    private final MapperUtils mapperUtils;

    private String retrievedContext;
    private TestContext testContext;

    public PromptData(MapperUtils mapperUtils) {
        this.mapperUtils = mapperUtils;
    }

    public String getRetrievedContext() {
        if (testContext == null || testContext.testContext() == null) {
            return null;
        }

        return testContext.testContext();
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


}
