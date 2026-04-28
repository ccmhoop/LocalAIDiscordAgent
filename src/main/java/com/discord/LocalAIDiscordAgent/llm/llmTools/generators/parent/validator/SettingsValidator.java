package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.validator;

public abstract class SettingsValidator<T extends Record> {

   public abstract boolean isUsable(T settings);

}
