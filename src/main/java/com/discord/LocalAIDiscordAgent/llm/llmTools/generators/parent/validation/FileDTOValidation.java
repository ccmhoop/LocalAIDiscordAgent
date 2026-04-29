package com.discord.LocalAIDiscordAgent.llm.llmTools.generators.parent.validation;

public abstract class FileDTOValidation<T extends Record> {

   public abstract boolean isUsable(T settings);

}
