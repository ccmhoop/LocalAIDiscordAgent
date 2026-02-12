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

CREATE TABLE IF NOT EXISTS TOOL_MEMORY
(
    conversation_id varchar(128) not null,
    content         text         not null,
    type            varchar(10)  not null
        constraint tool_memory_type_check check ((type)::text = ANY
                                                 ((ARRAY ['USER'::character varying, 'ASSISTANT'::character varying, 'SYSTEM'::character varying, 'TOOL'::character varying])::text[])),
    timestamp       timestamp    not null
)^

ALTER TABLE TOOL_MEMORY OWNER TO postgres^

CREATE INDEX IF NOT EXISTS tool_memory_conversation_id_timestamp_idx
    ON TOOL_MEMORY (conversation_id, timestamp)^

CREATE INDEX IF NOT EXISTS chat_memory_hnsw_idx
    ON VECTOR_STORE_CHAT_MEMORY USING HNSW (embedding vector_cosine_ops)^

CREATE INDEX IF NOT EXISTS web_search_hnsw_idx
    ON VECTOR_STORE_WEB_SEARCH_MEMORY USING HNSW (embedding vector_cosine_ops)^