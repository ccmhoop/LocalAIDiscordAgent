package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.validation;

public abstract class SettingsPayloadValidator<T extends Record> {

   public abstract boolean isUsable(T settings);

}
