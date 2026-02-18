package com.discord.LocalAIDiscordAgent.advisor.advisors;

import com.discord.LocalAIDiscordAgent.webSearch.helpers.WebSearchChunkMerger.MergedWebResults;
import lombok.Getter;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Map;

public class QwenVectorAdvisor extends AbstractQwenAdvisor {

    @Getter
    private String augmentedSystemMsg;
    private final String template;

    public QwenVectorAdvisor(PromptTemplate template) {
        this.template = template.getTemplate();
    }

    public void augmentSystemMsg(MergedWebResults merged, String beforeSystemMsg) {
        StringBuilder sb = new StringBuilder();
        for (var item : merged.results()) {
            sb.append(createDataBlock(item.rank(), item.content())).append("\n");
        }

        this.augmentedSystemMsg = beforeSystemMsg + PromptTemplate.builder()
                .template(template)
                .variables(Map.of(
                        "data", sb.toString().stripTrailing().substring(0, sb.length() - 2)
                ))
                .build()
                .render();
    }


    private String createDataBlock(int rank, String content) {
        return """
                {
                    similarity.score: "%s",
                    content: "%s",
                },
                """.formatted(
                rank,
                indentBlock(content)
        ).stripTrailing();
    }


}
