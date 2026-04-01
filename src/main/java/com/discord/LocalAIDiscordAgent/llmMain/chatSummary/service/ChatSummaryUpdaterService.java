package com.discord.LocalAIDiscordAgent.llmMain.chatSummary.service;

import com.discord.LocalAIDiscordAgent.llmMain.chatSummary.records.SummaryRecords.Evidence;
import com.discord.LocalAIDiscordAgent.llmMain.chatSummary.records.SummaryRecords.Fact;
import com.discord.LocalAIDiscordAgent.llmMain.chatSummary.records.SummaryRecords.FactCandidate;
import com.discord.LocalAIDiscordAgent.llmMain.chatSummary.records.SummaryRecords.FactsUpdate;
import com.discord.LocalAIDiscordAgent.llmMain.chatSummary.records.SummaryRecords.MemoryState;
import com.discord.LocalAIDiscordAgent.llmMain.chatSummary.records.SummaryRecords.SummaryUpdate;
import com.discord.LocalAIDiscordAgent.llmMain.chatSummary.records.SummaryRecords.Turn;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatSummaryUpdaterService {

    // Tune these for your model + prompt budget
    private static final int MIN_NEW_TURNS_TO_UPDATE = 2;
    private static final int MAX_TURNS_PER_UPDATE = 30;
    private static final int MAX_CHARS_PER_TURN = 800;
    private static final int MAX_EVIDENCE_EXCERPT_CHARS = 140;
    private static final double MIN_FACT_CONFIDENCE_TO_KEEP = 0.55;

    private final ChatClient memoryWorker;

    public ChatSummaryUpdaterService(ChatClient summaryChatClient) {
        this.memoryWorker = summaryChatClient;
    }

    /**
     * Rolling memory update using a timestamp watermark (lastSummarizedTs).
     * Works even if your recent window is trimmed.
     */
    public MemoryState updateIfNeeded(MemoryState current, List<Turn> turns) {

        MemoryState safeCurrent = normalizeCurrent(current);
        if (turns == null || turns.isEmpty()) return safeCurrent;

        // Defensive sort: timestamp asc, user before assistant for ties, then stable id
        List<Turn> ordered = new ArrayList<>(turns);
        ordered.sort(
                Comparator.comparing(Turn::ts, ChatSummaryUpdaterService::compareTsNullable)
                        .thenComparing(t -> "user".equalsIgnoreCase(nullToEmpty(t.role())) ? 0 : 1)
                        .thenComparing(t -> nullToEmpty(t.id()))
        );

        // Select only turns after watermark
        String watermark = safeCurrent.lastSummarizedTs();
        List<Turn> newTurns = (watermark == null || watermark.isBlank())
                ? ordered
                : ordered.stream()
                .filter(t -> compareTsNullable(nullToEmpty(t.ts()), watermark) > 0)
                .toList();

        if (newTurns.size() < MIN_NEW_TURNS_TO_UPDATE) return safeCurrent;

        // Batch to avoid huge prompts
        List<Turn> batch = newTurns.size() > MAX_TURNS_PER_UPDATE
                ? newTurns.subList(0, MAX_TURNS_PER_UPDATE)
                : newTurns;

        String newestBatchTs = maxTs(batch);

        SummaryUpdate su = computeRollingSummary(safeCurrent.summary(), batch);
        FactsUpdate fu = extractFacts(batch);
        List<Fact> extractedFacts = normalizeFactsFromModel(fu, batch);
        List<Fact> mergedFacts = mergeFacts(safeCurrent.facts(), extractedFacts);

        return new MemoryState(
                safe(su.summary()),
                mergedFacts,
                newestBatchTs
        );
    }


    private SummaryUpdate computeRollingSummary(String existingSummary, List<Turn> newTurns) {

        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(SummaryUpdate.class)
                .maxRepeatAttempts(3)
                .build();

        var summaryConv = new BeanOutputConverter<>(SummaryUpdate.class);

        String turnsText = renderTurnsForPrompt(newTurns);
        Map<String, Object> summarySchema = summaryConv.getJsonSchemaMap();

        SummaryUpdate modelOut = memoryWorker.prompt()
                .options(OllamaChatOptions.builder()
                        .format(summarySchema)   // <- key
                        .disableThinking()
                        .build())
                .system("""
                        You update chat memory as a rolling summary.
                        Output ONLY valid JSON matching the schema.
                        
                        Rules:
                        - Keep summary concise (<= 30 tokens).
                        - Store only durable conversation state:
                          - user goals
                          - user requests
                          - unresolved questions
                          - decisions made by the user
                          - stable preferences explicitly stated by the user
                          - important factual context introduced by the user
                        - Do NOT store assistant phrasing, jokes, tone, punchlines, wording, analogies, or stylistic choices.
                        - Do NOT store completed assistant answers as memory.
                        - Do NOT store one-off banter unless it changes future task handling.
                        - Prefer user intent over assistant response.
                        - If the new turns contain only casual banter with no durable value, preserve the existing summary or return an empty update.
                        - Paraphrase aggressively.
                        - Never quote long text.
                        - Do NOT include sensitive personal attributes.
                        """)
                .user(u -> u.text("""
                                New turns:
                                {turns}
                                
                                Always create a new summary using user messages!
                                Ignore assistant jokes, phrasing, and completed replies unless they create an unresolved task or a lasting decision.
                                """)
//                        .param("existing", safe(""))
                        .param("turns", turnsText))
                .advisors(validation)
                .call()
                .entity(SummaryUpdate.class);


        return new SummaryUpdate(safe(Objects.requireNonNull(modelOut).summary()));
    }

//    7. If the user is speaking casually, set conversation style to "casual",
    private FactsUpdate extractFacts(List<Turn> newTurns) {

        var validation = StructuredOutputValidationAdvisor.builder()
                .outputType(FactsUpdate.class)
                .maxRepeatAttempts(3)
                .build();

        var factsConv = new BeanOutputConverter<>(FactsUpdate.class);
        Map<String, Object> factsSchema = factsConv.getJsonSchemaMap();

        String turnsText = renderTurnsForPrompt(newTurns);

        FactsUpdate out = memoryWorker.prompt()
                .options(OllamaChatOptions.builder()
                        .format(factsSchema)
                        .disableThinking()
                        .build())
                .system("""
                        You are a memory extraction worker.
                        
                        Your job is to extract only durable, user-relevant facts from conversation turns.
                        
                        Core rules:
                        1. Return only facts that are useful in future turns.
                        2. Prefer normalized semantic facts over raw quotes.
                        3. Do not copy assistant phrasing into facts.
                        4. Do not store temporary acknowledgements like "yeah", "ok", "lol", or "sure".
                        5. Do not store completed assistant replies, jokes, or style as facts.
                        6. Only extract facts about:
                           - current topic
                           - conversation style
                           - user preference
                           - unresolved request
                           - stable requirement
                        7. Always update conversation style value accordingly to the new turns,
                        8. If the user is reflecting on identity through darkness / blackness imagery, normalize the topic to:
                           "identity and darkness imagery"
                        9. Use short reusable values, not long sentences.
                        10. Do not invent facts that are not grounded in the turns.
                        
                        Fact writing rules:
                        - key names must be snake_case
                        - prefer broad reusable keys
                        - preferred keys for this kind of chat:
                          - current_topic
                          - conversation_style
                          - user_preference
                          - unresolved_request
                          - stable_requirement
                        
                        Confidence rules:
                        - 0.95 for very explicit facts
                        - 0.90 for strong conversational inference
                        - 0.75 for weaker but still grounded inference
                        - do not output low-confidence guesses
     
                        """)
                .user(u -> u.text("""
                        Turns:
                        {turns}
                        
                        Extract only durable user-relevant facts.
                        Ignore assistant jokes, phrasing, and completed replies unless they represent a lasting user preference, requirement, or unresolved request.
                        """).param("turns", turnsText))
                .advisors(validation)
                .call()
                .entity(FactsUpdate.class);


        return Objects.requireNonNull(out);
    }

    /**
     * Convert model facts (with evidenceTurnIds) into persisted facts (with evidence excerpts).
     * This makes evidence stable even if your DB later trims/deletes turns.
     */
    private static List<Fact> normalizeFactsFromModel(FactsUpdate modelOut, List<Turn> batch) {
        List<FactCandidate> candidates = (modelOut == null || modelOut.facts() == null) ? List.of() : modelOut.facts();

        Map<String, Turn> byId = new HashMap<>();
        if (batch != null) {
            for (Turn t : batch) {
                if (t == null) continue;
                String id = safe(t.id()).trim();
                if (!id.isEmpty()) byId.put(id, t);
            }
        }

        return candidates.stream()
                .filter(Objects::nonNull)
                .filter(c -> !safe(c.key()).isBlank() && !safe(c.value()).isBlank())
                .map(c -> new Fact(
                        c.key().trim(),
                        c.value().trim(),
                        clamp01(c.confidence()),
                        buildEvidence(c.evidenceTurnIds(), byId)
                ))
                .filter(f -> f.confidence() >= MIN_FACT_CONFIDENCE_TO_KEEP)
                .limit(10)
                .toList();
    }

    private static List<Evidence> buildEvidence(List<String> evidenceTurnIds, Map<String, Turn> byId) {
        if (evidenceTurnIds == null || evidenceTurnIds.isEmpty()) return List.of();

        LinkedHashSet<String> uniq = new LinkedHashSet<>();
        for (String id : evidenceTurnIds) {
            if (id == null) continue;
            String x = id.trim();
            if (!x.isEmpty()) uniq.add(x);
        }

        List<Evidence> out = new ArrayList<>();
        for (String id : uniq) {
            Turn t = byId.get(id);
            if (t == null) continue; // ignore ids not present in this batch
            out.add(new Evidence(
                    id,
                    safe(t.ts()),
                    safe(t.role()),
                    safe(t.authorId()),
                    truncateEvidenceExcerpt(t.content())
            ));
        }
        return out;
    }

    private static List<Fact> mergeFacts(List<Fact> currentFacts, List<Fact> extractedFacts) {
        List<Fact> current = currentFacts == null ? List.of() : currentFacts;
        List<Fact> extracted = extractedFacts == null ? List.of() : extractedFacts;

        Map<String, Fact> map = new LinkedHashMap<>();
        for (Fact f : current) {
            if (f == null) continue;
            map.put(safe(f.key()), f);
        }

        for (Fact f : extracted) {
            if (f == null) continue;

            String key = safe(f.key());
            Fact prev = map.get(key);

            if (prev == null) {
                map.put(key, f);
                continue;
            }

            if (Objects.equals(safe(prev.value()), safe(f.value()))) {
                double bumped = Math.min(0.99, Math.max(prev.confidence(), f.confidence()) + 0.05);
                List<Evidence> ev = unionEvidence(prev.evidence(), f.evidence());
                map.put(key, new Fact(key, safe(f.value()), bumped, ev));
            } else {
                // Conflict: only replace if clearly stronger
                if (f.confidence() >= prev.confidence() + 0.15) {
                    map.put(key, f);
                } else {
                    // keep old but slightly degrade confidence (optional)
                    double lowered = Math.max(0.0, prev.confidence() - 0.20);
                    map.put(key, new Fact(
                            key,
                            safe(prev.value()),
                            lowered,
                            prev.evidence() == null ? List.of() : prev.evidence()
                    ));
                }
            }
        }

        return new ArrayList<>(map.values());
    }

    private static List<Evidence> unionEvidence(List<Evidence> a, List<Evidence> b) {
        Map<String, Evidence> map = new LinkedHashMap<>();
        if (a != null) {
            for (Evidence e : a) {
                if (e == null) continue;
                String id = safe(e.turnId()).trim();
                if (!id.isEmpty()) map.put(id, e);
            }
        }
        if (b != null) {
            for (Evidence e : b) {
                if (e == null) continue;
                String id = safe(e.turnId()).trim();
                if (!id.isEmpty()) map.put(id, e);
            }
        }
        return new ArrayList<>(map.values());
    }

    private static String renderTurnsForPrompt(List<Turn> turns) {
        return turns.stream()
                .map(t -> "%s | %s | %s | %s(%s): %s".formatted(
                        safe(t.id()),
                        safe(t.ts()),
                        safe(t.role()),
                        safe(t.authorName()),
                        safe(t.authorId()),
                        truncateForPrompt(t.content())
                ))
                .collect(Collectors.joining("\n"));
    }

    private static String truncateForPrompt(String s) {
        String x = safe(s).replace("\r", "").trim();
        if (x.length() <= MAX_CHARS_PER_TURN) return x;
        return x.substring(0, MAX_CHARS_PER_TURN) + " …(truncated)";
    }

    private static String truncateEvidenceExcerpt(String s) {
        String x = safe(s).replace("\r", "").trim();
        if (x.length() <= MAX_EVIDENCE_EXCERPT_CHARS) return x;
        return x.substring(0, MAX_EVIDENCE_EXCERPT_CHARS) + "…";
    }

    private static MemoryState normalizeCurrent(MemoryState current) {
        if (current == null) {
            return new MemoryState("", List.of(), null);
        }
        return new MemoryState(
                safe(current.summary()),
                current.facts() == null ? List.of() : current.facts(),
                blankToNull(current.lastSummarizedTs())
        );
    }

    private static String maxTs(List<Turn> turns) {
        String max = null;
        for (Turn t : turns) {
            String ts = safe(t.ts());
            if (max == null || compareTsNullable(ts, max) > 0) {
                max = ts;
            }
        }
        return max;
    }

    // ---- timestamp ordering (Instant / OffsetDateTime / LocalDateTime / fallback) ----

    private static int compareTsNullable(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;

        Instant ia = parseToInstant(a);
        Instant ib = parseToInstant(b);
        if (ia != null && ib != null) return ia.compareTo(ib);

        // Fallback: works if strings are consistently ISO-like
        return a.compareTo(b);
    }

    private static Instant parseToInstant(String ts) {
        if (ts == null || ts.isBlank()) return null;

        try {
            return Instant.parse(ts);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return OffsetDateTime.parse(ts).toInstant();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(ts).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
        }

        return null;
    }

    // ---- misc helpers ----

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        return Math.min(v, 1.0);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
