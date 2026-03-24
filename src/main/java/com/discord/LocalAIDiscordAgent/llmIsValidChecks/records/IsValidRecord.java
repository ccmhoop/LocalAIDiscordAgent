package com.discord.LocalAIDiscordAgent.llmIsValidChecks.records;

import java.util.List;

public record IsValidRecord(
        String systemMsg,
        List<String> instruction,
        IsValidContextRecord dataToCheck
) {
}
