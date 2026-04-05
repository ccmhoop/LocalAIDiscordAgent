package com.discord.LocalAIDiscordAgent.interactionProcessor;

import com.discord.LocalAIDiscordAgent.llmClients.chatSummary.service.ChatSummaryService;
import com.discord.LocalAIDiscordAgent.llmClients.chatSummary.records.SummaryRecords.Turn;
import com.discord.LocalAIDiscordAgent.chatMemory.chatMemory.groupChatMemory.model.GroupChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.chatMemory.groupChatMemory.service.GroupChatMemoryService;
import com.discord.LocalAIDiscordAgent.chatMemory.chatMemory.recentChatMemory.model.RecentChatMemory;
import com.discord.LocalAIDiscordAgent.chatMemory.chatMemory.recentChatMemory.service.RecentChatMemoryService;
import com.discord.LocalAIDiscordAgent.discord.data.DiscGlobalData;
import com.discord.LocalAIDiscordAgent.user.model.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.springframework.ai.chat.messages.MessageType.ASSISTANT;
import static org.springframework.ai.chat.messages.MessageType.USER;

@Slf4j
@Component
public class ProcessSummaryClient {

    private final ChatSummaryService chatSummaryService;
    private final RecentChatMemoryService recentService;
    private final GroupChatMemoryService groupService;
    private final DiscGlobalData discGlobalData;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long BASE_DELAY_MS = 100;

    public ProcessSummaryClient(ChatSummaryService chatSummaryService,
                                RecentChatMemoryService recentService,
                                GroupChatMemoryService groupService, DiscGlobalData discGlobalData) {
        this.chatSummaryService = chatSummaryService;
        this.recentService = recentService;
        this.groupService = groupService;
        this.discGlobalData = discGlobalData;
    }

    /**
     * Updates both:
     * - per-user summary (guild:user)
     * - per-channel group summary (guild:channel)
     *
     * IMPORTANT: turns use STABLE ids (DB id / snowflake), not array indices.
     */
    public void saveInteraction() {
        String userConversationId = discGlobalData.getConversationId();
        String groupConversationId = discGlobalData.getGroupConversationId();

        int attempts = 0;
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                performSaveInteraction(userConversationId, groupConversationId);
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                attempts++;
                if (attempts >= MAX_RETRY_ATTEMPTS) {
                    log.error("Failed to update summaries after {} attempts (userConv={}, groupConv={})",
                            MAX_RETRY_ATTEMPTS, userConversationId, groupConversationId, e);
                    throw e;
                }

                long delay = BASE_DELAY_MS * (1L << (attempts - 1));
                log.warn("Optimistic locking failure on attempt {} (userConv={}, groupConv={}). Retrying in {}ms",
                        attempts, userConversationId, groupConversationId, delay);

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry delay", ie);
                }
            }
        }
    }

    private void performSaveInteraction(String userConversationId, String groupConversationId) {

        // --------------------
        // 1) Per-user summary
        // --------------------
        Map<MessageType, List<RecentChatMemory>> recentMap = recentService.getChatMemoryAsMap();

        List<RecentChatMemory> recentUsers = recentMap.getOrDefault(USER, List.of());
        List<RecentChatMemory> recentAssistants = recentMap.getOrDefault(ASSISTANT, List.of());

        List<Turn> userTurns = buildTurnsFromRecent(recentUsers, recentAssistants);
        if (!userTurns.isEmpty()) {
            chatSummaryService.updateIfNeeded(userConversationId, userTurns);
        }

        // --------------------
        // 2) Group/channel summary
        // --------------------
        // NOTE: GroupChatMemoryAdvisor expects this service method signature.
        Map<MessageType, List<GroupChatMemory>> groupMap = groupService.getChatMemoryAsMap();
        List<GroupChatMemory> groupUsers = groupMap.getOrDefault(USER, List.of());
        List<GroupChatMemory> groupAssistants = groupMap.getOrDefault(ASSISTANT, List.of());

        List<Turn> groupTurns = buildTurnsFromGroup(groupUsers, groupAssistants);
        if (!groupTurns.isEmpty()) {
            chatSummaryService.updateIfNeeded(groupConversationId, groupTurns);
        }
    }

    private static List<Turn> buildTurnsFromRecent(List<RecentChatMemory> user, List<RecentChatMemory> assistant) {
        record Tmp(String id, String ts, String role, String authorId, String authorName, String content) {}

        List<Tmp> tmp = new ArrayList<>(user.size() + assistant.size());

        for (RecentChatMemory m : user) {
            tmp.add(new Tmp(
                    String.valueOf(m.getId()),
                    String.valueOf(m.getTimestamp()),
                    "user",
                    safeUserId(m),
                    safeUserName(m),
                    m.getContent()
            ));
        }

        for (RecentChatMemory m : assistant) {
            tmp.add(new Tmp(
                    String.valueOf(m.getId()),
                    String.valueOf(m.getTimestamp()),
                    "assistant",
                    "assistant",
                    "assistant",
                    m.getContent()
            ));
        }

        tmp.sort(Comparator.comparing(Tmp::ts)
                .thenComparing(t -> "user".equals(t.role()) ? 0 : 1)
                .thenComparing(Tmp::id));

        List<Turn> turns = new ArrayList<>(tmp.size());
        for (Tmp t : tmp) {
            turns.add(new Turn(t.id(), t.ts(), t.role(), t.authorId(), t.authorName(), t.content()));
        }
        return turns;
    }

    private static List<Turn> buildTurnsFromGroup(List<GroupChatMemory> user, List<GroupChatMemory> assistant) {
        record Tmp(String id, String ts, String role, String authorId, String authorName, String content) {}

        List<Tmp> tmp = new ArrayList<>(user.size() + assistant.size());

        for (GroupChatMemory m : user) {
            tmp.add(new Tmp(
                    String.valueOf(m.getId()),
                    String.valueOf(m.getTimestamp()),
                    "user",
                    safeUserId(m.getUser()),
                    safeUserName(m.getUser()),
                    m.getContent()
            ));
        }

        for (GroupChatMemory m : assistant) {
            tmp.add(new Tmp(
                    String.valueOf(m.getId()),
                    String.valueOf(m.getTimestamp()),
                    "assistant",
                    "assistant",
                    "assistant",
                    m.getContent()
            ));
        }

        tmp.sort(Comparator.comparing(Tmp::ts)
                .thenComparing(t -> "user".equals(t.role()) ? 0 : 1)
                .thenComparing(Tmp::id));

        List<Turn> turns = new ArrayList<>(tmp.size());
        for (Tmp t : tmp) {
            turns.add(new Turn(t.id(), t.ts(), t.role(), t.authorId(), t.authorName(), t.content()));
        }
        return turns;
    }

    private static String safeUserId(RecentChatMemory m) {
        if (m != null && m.getUser() != null && m.getUser().getUserId() != null) return String.valueOf(m.getUser().getUserId());
        return "";
    }

    private static String safeUserName(RecentChatMemory m) {
        if (m != null && m.getUser() != null) {
            String nick = m.getUser().getServerNickname();
            if (nick != null && !nick.isBlank()) return nick;
            String global = m.getUser().getUserGlobal();
            if (global != null && !global.isBlank()) return global;
        }
        return "";
    }

    private static String safeUserId(UserEntity u) {
        return u == null || u.getUserId() == null ? "" : String.valueOf(u.getUserId());
    }

    private static String safeUserName(UserEntity u) {
        if (u == null) return "";
        if (u.getServerNickname() != null && !u.getServerNickname().isBlank()) return u.getServerNickname();
        return u.getUserGlobal() == null ? "" : u.getUserGlobal();
    }
}
