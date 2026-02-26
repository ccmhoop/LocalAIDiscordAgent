-- Use '^' as the delimiter in application.properties: spring.sql.init.separator=^
CREATE EXTENSION IF NOT EXISTS "uuid-ossp"^
CREATE EXTENSION IF NOT EXISTS vector^

CREATE TABLE IF NOT EXISTS VECTOR_STORE_CHAT_MEMORY
(
    id        uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content   TEXT,
    metadata  JSON,
    embedding VECTOR(1024)
)^

CREATE TABLE IF NOT EXISTS VECTOR_STORE_WEB_SEARCH_MEMORY
(
    id        uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content   TEXT,
    metadata  JSON,
    embedding VECTOR(1024)
)^

CREATE INDEX IF NOT EXISTS chat_memory_hnsw_idx
    ON VECTOR_STORE_CHAT_MEMORY USING HNSW (embedding vector_cosine_ops)^

CREATE INDEX IF NOT EXISTS web_search_hnsw_idx
    ON VECTOR_STORE_WEB_SEARCH_MEMORY USING HNSW (embedding vector_cosine_ops)^