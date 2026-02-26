package com.discord.LocalAIDiscordAgent.advisor.helpers;

public class AdvisorHelper {

    public static String indentLines(String s, int indentAmount) {
        if (s == null || s.isBlank()) return "";

        String[] lines = s.split("\\R", -1);
        StringBuilder out = new StringBuilder();
        if (indentAmount <= 0) {
            for (String line : lines) {
                if (line.isBlank()) continue;
                out.append(line.stripLeading());
            }
        } else {
            String indents = "\t".repeat(indentAmount);
            for (String line : lines) {
                if (line.isBlank()) continue;
                out.append(indents);
                out.append(line);
            }
        }
        return out.toString();
    }

//    public static String indentBlock(String s, int indentAmount) {
//        if (s == null || s.isBlank()) return "";
//        StringBuilder out = new StringBuilder();
//        String indents = "\t".repeat(indentAmount);
//        out.append(indents);
//        return  """
//                """
//                + indents + "%s".formatted(s) +
//                """
//                 """;
//    }

    public static String indentBlock(String s, int indentAmount, boolean isTabIndent) {
        if (s == null || s.isBlank()) return "";
        if (isTabIndent) {
            return  s.indent(indentAmount*4).stripTrailing();
        }
        return  s.indent(indentAmount).stripTrailing();
    }
}



