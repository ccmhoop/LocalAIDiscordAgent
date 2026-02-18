package com.discord.LocalAIDiscordAgent.advisor.advisors;

public class AbstractQwenAdvisor {

    public static String indentBlock(String s) {
        if (s == null || s.isBlank()) return "";
        String[] lines = s.split("\\R", -1);
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            if (line.isBlank()) continue;
//            out.append("")
            out.append(line.stripLeading());
        }
        return out.toString().stripTrailing();
    }
}
