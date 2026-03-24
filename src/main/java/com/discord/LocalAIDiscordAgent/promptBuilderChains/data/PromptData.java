package com.discord.LocalAIDiscordAgent.promptBuilderChains.data;

import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import lombok.Getter;
import org.springframework.stereotype.Component;


@Getter
@Component
public class PromptData  {

    private final MapperUtils mapperUtils;

    private String retrievedContext;

    public PromptData(MapperUtils mapperUtils) {
        this.mapperUtils = mapperUtils;
    }

    public void setRetrievedContext(Record contextRecord){
        this.retrievedContext = mapperUtils.valuesToString(contextRecord);
    }

    public void setRetrievedContext(String contextString){
        this.retrievedContext = contextString;
    }

    public boolean isRetrievedContextPresent() {
        return retrievedContext != null && !retrievedContext.isBlank();
    }


}
