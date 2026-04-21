package com.discord.LocalAIDiscordAgent.memory.chatMemory.webChatMemory.service;

import org.springframework.stereotype.Service;

@Service
public class WebChatMemoryService {
//
//    private final WebChatMemoryRepository repo;
//    private final UserRepository userRepo;
//
//    public WebChatMemoryService(WebChatMemoryRepository webChatMemoryRepository, UserRepository userRepo) {
//        this.repo = webChatMemoryRepository;
//        this.userRepo = userRepo;
//    }
//
//    @Transactional
//    public void save(List<Message> messages, UserEntity user){
//        repo.deleteAllByUser(user);
//        repo.flush();
//        List<WebChatMemory> chatMemories = createSaveAllList(messages, user);
//        repo.saveAll(chatMemories);
//    }
//
//    public Map<MessageType, List<WebChatMemory>> getChatMemoryAsMap() {
//        UserEntity userEntity = userRepo.findByUserId(Long.parseLong(discGlobalData.getUserId()));
//        var partitioned = repo.findAllByUser(userEntity).stream()
//                .filter(m -> m.getType() == USER || m.getType() == ASSISTANT )
//                .collect(Collectors.partitioningBy(
//                        m -> m.getType() == USER
//                ));
//
//        if (partitioned.get(true).isEmpty() || partitioned.get(false).isEmpty()) {
//            return Collections.emptyMap();
//        }
//
//        return Map.of(
//                USER, partitioned.get(true),
//                ASSISTANT, partitioned.get(false));
//    }
//
//    private List<WebChatMemory> createSaveAllList(List<Message> messages, UserEntity user) {
//        return messages.stream().map(m -> buildChatMemory( m, user)).collect(Collectors.toCollection(ArrayList::new));
//    }
//
//    private WebChatMemory buildChatMemory(Message message, UserEntity user){
//        return WebChatMemory.builder()
//                .type(message.getMessageType())
//                .user(user)
//                .guildId(discGlobalData.getGuildId())
//                .content(message.getText())
//                .channelId(discGlobalData.getChannelId())
//                .timestamp(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
//                .conversationId(discGlobalData.getConversationId())
//                .build();
//    }

}
