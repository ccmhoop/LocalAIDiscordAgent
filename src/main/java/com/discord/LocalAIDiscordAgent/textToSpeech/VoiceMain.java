package com.discord.LocalAIDiscordAgent.textToSpeech;

import lombok.extern.slf4j.Slf4j;
import org.pitest.voices.*;
import org.pitest.voices.audio.Audio;
import org.pitest.voices.g2p.core.Dictionary;
import org.pitest.voices.kokoro.KokoroModels;
import org.pitest.voices.uk.EnUkDictionary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.pitest.voices.ChorusConfig.chorusConfig;


@Slf4j
public class VoiceMain {

    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final Pattern SPACE_BEFORE_PUNCT = Pattern.compile("\\s+([,.;:!?])");
    private static final Pattern MISSING_SPACE_AFTER_PUNCT = Pattern.compile("([,.;:!?])(\\S)");
    private static final Pattern SPACED_HYPHEN = Pattern.compile("\\s-\\s");
    private static final Pattern MULTI_DASH = Pattern.compile("\\s--+\\s");
    private static final Pattern TRAILING_G_DROPPING = Pattern.compile("\\b([A-Za-z]{3,})in'\\b");
    private static final Pattern CURLY_APOS = Pattern.compile("[‘’ʼ′]");
    private static final Pattern CURLY_QUOTES = Pattern.compile("[“”]");
    private static final Pattern UNICODE_DASHES = Pattern.compile("[‒–—−]"); // figure/en/em/minus
    private static final Pattern S_ENDING_POSSESSIVE = Pattern.compile("\\b([A-Za-z]+s)'s\\b");

    public static void generateAndSaveAudio(String text, String userId) {
        try {
            // Use the specified desktop directory
            Path audioDir = Paths.get("savedSpeech");

            // Create directory if it doesn't exist
            if (!Files.exists(audioDir)) {
                Files.createDirectories(audioDir);
            }

            // Create unique filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("response_%s_%s.wav", userId, timestamp);
            Path audioPath = audioDir.resolve(filename);

            Dictionary dict = EnUkDictionary.en_uk().withAdditions(ScottishDictionary.glasgow_Additions);
//            dict = EnUkDictionary.en_uk();
            ChorusConfig config = chorusConfig(dict);
            try (Chorus chorus = new Chorus(config)) {
                Voice v1 = chorus.voice(KokoroModels.bmGeorge())
                        .withSpeed(1.15f)
                        .withStress(Stresses.NO_STRESS)
                        .withPauses(List.of(
                                new Pause(",", 1),
                                new Pause("'", 2),
                                new Pause(".", 2)
                        ));

                Audio audio = sayInSentenceChunks(v1, text);
                float factor = 1.25f;
                Audio pitchedAndSlower = new Audio(audio.getSamples(), Math.round(audio.getSampleRate() * factor));
                pitchedAndSlower.save(audioPath);
            }
        } catch (IOException e) {
            log.error("Error saving audio file: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error generating audio: {}", e.getMessage(), e);
        }
    }


    public static Audio sayInSentenceChunks(Voice v, String text) {
        if (text == null) text = "";
        String t = text.trim();

        t = sanitizeForTts(t, false, true);


        if (t.isEmpty()) return v.say("");

        List<String> chunks = new ArrayList<>();
        StringBuilder cur = new StringBuilder();

        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            cur.append(ch);

            // chunk ONLY when we hit '.', '?', '!' (but treat "..." as one terminator)
            if (ch == '.' || ch == '?' || ch == '!') {

                // if we're in an ellipsis, don't chunk yet
                if (ch == '.' && i + 1 < t.length() && t.charAt(i + 1) == '.') {
                    continue;
                }

                // consume remaining dots in an ellipsis so we chunk once at the end
                if (ch == '.') {
                    while (i + 1 < t.length() && t.charAt(i + 1) == '.') {
                        i++;
                        cur.append('.');
                    }
                }

                String chunk = cur.toString().trim();
                if (!chunk.isEmpty()) chunks.add(chunk);
                cur.setLength(0);
            }
        }

        String tail = cur.toString().trim();
        if (!tail.isEmpty()) chunks.add(tail);

        Audio out = null;
        for (String c : chunks) {
            Audio part = v.say(c);
            out = (out == null) ? part : out.append(part);
        }
        return out;
    }
//    public static String sanitizeForTts(String raw) {
//        return sanitizeForTts(raw, false, true);
//    }

    /**
     * @param preferDialectDa  if true, keep "da"; if false, map "da" -> "the"
     * @param splitLongClauses if true, splits "... , and ..." into ". And ..." for better cadence
     */
    public static String sanitizeForTts(String raw, boolean preferDialectDa, boolean splitLongClauses) {
        if (raw == null || raw.isBlank()) return "";

        String s = Normalizer.normalize(raw, Normalizer.Form.NFKC);

        // Unify apostrophes/quotes/dashes
        s = CURLY_APOS.matcher(s).replaceAll("'");
        s = CURLY_QUOTES.matcher(s).replaceAll("\"");
        s = UNICODE_DASHES.matcher(s).replaceAll("—");

        // Dashes
        s = MULTI_DASH.matcher(s).replaceAll(" — ");
        s = SPACED_HYPHEN.matcher(s).replaceAll(" — ");

        // Fix g-dropping: screamin' -> screaming
        s = TRAILING_G_DROPPING.matcher(s).replaceAll("$1ing");

        s = S_ENDING_POSSESSIVE.matcher(s).replaceAll("$1es");

        // Optional: da -> the
        if (!preferDialectDa) {
            s = s.replaceAll("\\bda\\b", "the");
        }

        // Optional cadence improvement: split long ", and ..." chains
        if (splitLongClauses) {
            s = s.replaceAll(",\\s+and\\s+", ". And ");
        }

        // Clean punctuation spacing
        s = SPACE_BEFORE_PUNCT.matcher(s).replaceAll("$1");
        s = MISSING_SPACE_AFTER_PUNCT.matcher(s).replaceAll("$1 $2");

        // Collapse whitespace
        s = MULTI_SPACE.matcher(s).replaceAll(" ").trim();

        return s;
    }

}
