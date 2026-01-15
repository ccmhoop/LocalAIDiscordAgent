CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS VECTOR_STORE_SCOTTISH_AGENT(
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content TEXT,
    metadata JSON,
    embedding VECTOR(1024)
);

CREATE TABLE IF NOT EXISTS SCOTTISH_AI_CHAT_MEMORY
(
    conversation_id varchar(128) not null,
    content         text        not null,
    type            varchar(10) not null
        constraint spring_ai_chat_memory_type_check
            check ((type)::text = ANY
                   ((ARRAY ['USER'::character varying, 'ASSISTANT'::character varying, 'SYSTEM'::character varying, 'TOOL'::character varying])::text[])),
    timestamp       timestamp   not null
);

alter table SCOTTISH_AI_CHAT_MEMORY
    owner to postgres;

create index IF NOT EXISTS spring_ai_chat_memory_conversation_id_timestamp_idx
    on SCOTTISH_AI_CHAT_MEMORY (conversation_id, timestamp);

CREATE INDEX ON VECTOR_STORE_SCOTTISH_AGENT USING HNSW (embedding vector_cosine_ops);