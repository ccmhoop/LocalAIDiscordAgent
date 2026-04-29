package com.discord.LocalAIDiscordAgent.llm.llmChains.data;

import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.imageGenerator.dto.ImageSettingsDTO;
import com.discord.LocalAIDiscordAgent.memory.chatMemory.records.ChatMemoryPayload;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.musicGenerator.dto.MusicSettingsDTO;
import com.discord.LocalAIDiscordAgent.llm.llmTools.generators.children.videoGenerator.dto.VideoSettingsDTO;
import com.discord.LocalAIDiscordAgent.objectMapper.MapperUtils;
import com.discord.LocalAIDiscordAgent.llm.llmTools.webSearch.records.WebSearchRecords.MergedWebQAItem;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Getter
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PromptData {

    private final MapperUtils mapperUtils;

    private TestContext testContext;
    private List<MergedWebQAItem> vectorDBResults;
    private String summary;
    private String queryString;
    private ChatMemoryPayload chatMemoryPayload;
    private ImageSettingsDTO imageSettingsDTO;
    private VideoSettingsDTO videoSettings;
    private boolean webSearchRequired;
    private MusicSettingsDTO musicSettings;

    public PromptData(MapperUtils mapperUtils) {
        this.mapperUtils = mapperUtils;
    }

    public void setMusicSettings(MusicSettingsDTO musicSettings) {
        this.musicSettings = musicSettings;
    }

    public MusicSettingsDTO getMusicSettings() {
        return musicSettings;
    }

    public void setVideoSettings(VideoSettingsDTO videoSettings) {
        this.videoSettings = videoSettings;
    }

    public VideoSettingsDTO getVideoSettings() {
        return videoSettings;
    }

    public void setWebSearchRequired(boolean webSearchRequired) {
        this.webSearchRequired = webSearchRequired;
    }

    public boolean isWebSearchRequired() {
        return webSearchRequired;
    }

    public void setImageSettingsDTO(ImageSettingsDTO imageSettingsDTO) {
        this.imageSettingsDTO = imageSettingsDTO;
    }

    public ImageSettingsDTO getImageSettingsDTO() {
        return imageSettingsDTO;
    }

    public void setChatMemoryPayload(ChatMemoryPayload chatMemoryPayload) {
        this.chatMemoryPayload = chatMemoryPayload;
    }

    public void setQueryString(Supplier<String> supplier) {
        try {
            this.queryString = normalize(supplier.get());
        } catch (Exception e) {
            log.error("Error executing query supplier", e);
            this.queryString = null;
        }
    }

    public void setQueryString(String queryString) {
        this.queryString = normalize(queryString);
    }

    public String getRetrievedContext() {
        return testContext == null ? null : normalize(testContext.testContext());
    }

    public void setSummary(String summary) {
        this.summary = normalize(summary);
    }

    public List<MergedWebQAItem> getVectorDBMemory() {
        return vectorDBResults;
    }

    public void setVectorDBResults(List<MergedWebQAItem> vectorDBResults) {
        this.vectorDBResults = (vectorDBResults == null || vectorDBResults.isEmpty())
                ? null
                : List.copyOf(vectorDBResults);
    }

    public void setRetrievedContext(Record contextRecord) {
        if (contextRecord == null) {
            this.testContext = null;
            return;
        }

        String value = normalize(mapperUtils.valuesToString(contextRecord));
        this.testContext = value == null ? null : new TestContext(value);
    }

    public void setRetrievedContext(String contextString) {
        String value = normalize(contextString);
        this.testContext = value == null ? null : new TestContext(value);
    }

    public boolean isRetrievedContextPresent() {
        return getRetrievedContext() != null;
    }

    public boolean hasSummary() {
        return summary != null;
    }

    public boolean hasQueryString() {
        return queryString != null;
    }

    public boolean hasVectorDbResults() {
        return vectorDBResults != null && !vectorDBResults.isEmpty();
    }

    public boolean hasChatMemoryPayload() {
        return chatMemoryPayload != null;
    }

    public void clear() {
        this.testContext = null;
        this.vectorDBResults = null;
        this.summary = null;
        this.queryString = null;
        this.chatMemoryPayload = null;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    public record TestContext(String testContext) {
    }

    public record VectorDBMemory(List<MergedWebQAItem> vectorDBResults) {
    }
}