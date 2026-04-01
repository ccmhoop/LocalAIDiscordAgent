package com.discord.LocalAIDiscordAgent.llmMemory.advisorRequests.booleanLLM;


import com.discord.LocalAIDiscordAgent.llmAdvisors.booleanLLM.records.BooLeanLMMContextRecord;
import com.discord.LocalAIDiscordAgent.llmAdvisors.booleanLLM.request.BooleanLLMRequest;
import com.discord.LocalAIDiscordAgent.webSearch.records.WebSearchRecords.MergedWebQAItem;

import java.util.List;

public class IsImageContextValidRequest extends BooleanLLMRequest {

    private static final String SYSTEM_MESSAGE = """
            You are a lenient context-validity classifier for summarization.
            
            Your task is to decide whether the retrieved memory inside <memory> is relevant enough to be used for summarization based on the user_message.
            
            Decision policy:
            - Be lenient.
            - Prefer accepting the memory when it is meaningfully related to the user_message.
            - Accept partial matches when they could still help produce a useful summary.
            - Accept the memory even if it is incomplete, indirect, or not strongly visual, as long as it is still relevant.
            - Do not reject only because the memory lacks detailed visual description.
            - Do not reject only because the overlap is semantic or topical rather than exact wording.
            
            Rules:
            1. If <memory> is empty, mark it as invalid.
            2. Mark the memory as valid when it matches the user_message by one or more of the following:
               - same person
               - same object
               - same place
               - same event
               - same scene
               - same topic
               - closely related wording or semantic meaning
            3. Mark the memory as valid when it would help produce even a partial or rough summary.
            4. Mark the memory as invalid only when it is:
               - clearly unrelated
               - about the wrong subject or event
               - too disconnected to help summarization
               - likely misleading for the user_message
            5. Ignore URLs, formatting noise, and other non-textual clutter when judging relevance.
            6. Do not require strong visual detail.
            7. Do not require exact keyword overlap.
            8. Semantic similarity and topical alignment are enough for acceptance in borderline cases.
            9. When uncertain, prefer marking the memory as valid.
            
            Allowed reason values:
            - empty
            - related
            - partial_match
            - semantic_match
            - unrelated
            - misleading
            
            <memory>
            %s
            </memory>
            """;

    public IsImageContextValidRequest(List<MergedWebQAItem> vectorDBMemory) {
        super(SYSTEM_MESSAGE, new BooLeanLMMContextRecord(
                null,
                null,
                null,
                vectorDBMemory,
                null
        ));
    }

}

//            4. Do not return accept based only on superficial visual similarity, keyword overlap, or vague topical relation.
//            5. Return accept only if the image context is useful for identifying, clarifying, or supporting important summary-worthy content such as:
//        - the main subject
//               - important objects or people
//               - visible actions or events
//               - scene or setting
//               - text shown in the image when clearly represented in the retrieved context
//               - relationships between key visual elements
//            6. decline when the image context is:
//        - unrelated
//               - only loosely related
//               - too generic
//               - too incomplete to support a useful summary
//               - visually ambiguous
//               - contextually mismatched
//               - low-value for summarization
//               - likely misleading for the content being summarized
//            7. Prefer decline over accept unless the retrieved image context would clearly improve the summary.
//        8. Judge validity based on practical summarization usefulness, not just whether the image context appears relevant at a surface level.