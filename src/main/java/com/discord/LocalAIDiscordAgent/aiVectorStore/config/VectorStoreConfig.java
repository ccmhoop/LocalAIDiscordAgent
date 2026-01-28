package com.discord.LocalAIDiscordAgent.aiVectorStore.config;

import com.discord.LocalAIDiscordAgent.aiVectorStore.init.VectorStoreDocuments;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore vectorStoreScottishConfig(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {

        PgVectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1024)                    // Optional: defaults to model dimensions or 1536
                .distanceType(COSINE_DISTANCE)       // Optional: defaults to COSINE_DISTANCE
                .indexType(HNSW)                     // Optional: defaults to HNSW
                .initializeSchema(true)              // Optional: defaults to false
                .schemaName("public")                // Optional: defaults to "public"
                .vectorTableName("VECTOR_STORE_SCOTTISH_AGENT")     // Optional: defaults to "vector_store"
                .maxDocumentBatchSize(10000)         // Optional: defaults to 10000
                .build();

        if (vectorStore.similaritySearch("Hello").isEmpty()) {
            vectorStore.add(VectorStoreDocuments.subjectKierDocuments());
        }

        return vectorStore;
    }

    @Bean
    public VectorStore vectorStoreChatMemory(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1024)                    // Optional: defaults to model dimensions or 1536
                .distanceType(COSINE_DISTANCE)       // Optional: defaults to COSINE_DISTANCE
                .indexType(HNSW)                     // Optional: defaults to HNSW
                .initializeSchema(true)              // Optional: defaults to false
                .schemaName("public")                // Optional: defaults to "public"
                .vectorTableName("VECTOR_STORE_CHAT_MEMORY")     // Optional: defaults to "vector_store"
                .maxDocumentBatchSize(10000)         // Optional: defaults to 10000
                .build();
    }

    @Bean
    public VectorStore vectorStoreWebSearchMemory(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1024)
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(true)
                .schemaName("public")
                .vectorTableName("VECTOR_STORE_WEB_SEARCH_MEMORY")
                .maxDocumentBatchSize(10000)
                .build();
    }

}
