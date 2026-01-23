package com.discord.LocalAIDiscordAgent.aiVectorStore.init;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static com.discord.LocalAIDiscordAgent.aiMemoryRetrieval.enums.MemoryTier.*;

public final class VectorStoreDocuments {

    public static List<Document> subjectKierDocuments() {

        return List.of(

        /* =========================
           BACKGROUND — stable facts
           ========================= */

                new Document(
                        "Real name is Conner. Lives in the netherlands.",
                        Map.of(
                                "tier", BACKGROUND,
                                "userId", "bigherc4024",
                                "subject", "conner",
                                "aliases", List.of("conner", "conner's", "conners","bigherc4024"
                                )
                        )
                ),

                new Document(
                        "Real name is Niall. Works as a geophysicist with experience in sonar.",
                        Map.of(
                                "tier", "BACKGROUND",
                                "userId", "kingofthesquirrels",
                                "subject", "Niall",
                                "aliases", List.of("niall", "niall","niall's", "king of the squirrels", "kingofthesquirrels")
                        )
                ),

                new Document(
                        "Real name is Sean. Lives in Glasgow and works as a security guard.",
                        Map.of(
                                "tier", BACKGROUND,
                                "userId", "killamushroom88",
                                "subject", "Sean",
                                "aliases", List.of(
                                        "sean",
                                        "seans",
                                        "sean's",
                                        "killamushroom88",
                                        "killa mushroom"
                                )
                        )
                ),

                new Document(
                        "Real name is Christopher Heaney. From Limerick, Ireland.",
                        Map.of(
                                "tier", BACKGROUND,
                                "userId", "murigrim",
                                "subject", "Christopher Heaney",
                                "aliases", List.of(
                                        "chris",
                                        "christopher",
                                        "murigrim"
                                )
                        )
                ),

                new Document(
                        "Real name is Charlie Basford. Lives in Nottingham.",
                        Map.of(
                                "tier", BACKGROUND,
                                "userId", "toxicrebel0627",
                                "subject", "Charlie Basford",
                                "aliases", List.of(
                                        "charlie",
                                        "basford",
                                        "charlies",
                                        "charlie's",
                                        "toxicrebel0627"
                                )
                        )
                ),

                new Document(
                        "Real name is Kier Scarr. From Glasgow.",
                        Map.of(
                                "tier", BACKGROUND,
                                "userId", "mrhightimes420",
                                "subject", "Kier Scarr",
                                "aliases", List.of(
                                        "kier",
                                        "mrhightimes420"
                                )
                        )
                ),

        /* =========================
           PERSONALITY — enduring traits
           ========================= */

                new Document(
                        "You own a dog named Cali, a Staffordshire Bull Terrier mix.",
                        Map.of(
                                "tier", PERSONALITY,
                                "userId", "kier",
                                "subject", "dog",
                                "aliases", List.of(
                                        "cali",
                                        "dog"
                                )
                        )
                ),

                new Document(
                        "You tend to communicate directly and prefer informal, conversational exchanges.",
                        Map.of(
                                "tier", PERSONALITY,
                                "userId", "kier",
                                "subject", "communication style",
                                "aliases", List.of(
                                        "direct",
                                        "informal",
                                        "straightforward"
                                )
                        )
                ),

                new Document(
                        "You are comfortable with ongoing conversations that assume shared context.",
                        Map.of(
                                "tier", PERSONALITY,
                                "userId", "kier",
                                "subject", "conversation flow",
                                "aliases", List.of(
                                        "ongoing conversation",
                                        "shared context"
                                )
                        )
                ),

        /* =========================
           SITUATIONAL — recurring patterns
           ========================= */

                new Document(
                        "Group conversations often involve overlapping replies and rapid topic shifts.",
                        Map.of(
                                "tier", SITUATIONAL,
                                "userId", "discord-group",
                                "subject", "group chat dynamics",
                                "aliases", List.of(
                                        "group chat",
                                        "discord",
                                        "overlapping conversation"
                                )
                        )
                ),

                new Document(
                        "Christopher is frequently mentioned in casual discussion.",
                        Map.of(
                                "tier", SITUATIONAL,
                                "userId", "murigrim",
                                "subject", "frequent topic",
                                "aliases", List.of(
                                        "chris",
                                        "christopher"
                                )
                        )
                ),

                new Document(
                        "Conversations often continue without formal greetings or conclusions.",
                        Map.of(
                                "tier", SITUATIONAL,
                                "userId", "kier",
                                "subject", "conversation entry",
                                "aliases", List.of(
                                        "no greeting",
                                        "mid-conversation"
                                )
                        )
                )
        );
    }

}
