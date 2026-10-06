-- ============================================================================
-- ORAZAKA — Local DB bootstrap · 40 — KNOWLEDGE CONTEXT (RAG sources)
-- ----------------------------------------------------------------------------
-- Owner: Knowledge service — RAG retrieval + tool source lookup + async indexing,
-- in its OWN database (orazaka_knowledge_db) under its own role since Phase 4.
-- MCP registry + tool cache/config are router-hosted (MCP execution) and live in
-- 30-jobs-config.sql; only the RAG *sources* the knowledge service owns are here.
-- The pgvector extension is created for the (currently dormant) vector store.
-- user_id columns are OPAQUE ActorIds — no FK into the identity context.
-- ============================================================================

-- The password is NOT set here. psql 15 cannot read the environment (\getenv is 16+)
-- and ERR-125 bans a shell script, so `orazaka start` applies ALTER ROLE from
-- KNOWLEDGE_DB_PASSWORD once the container is healthy. A role created without a
-- password cannot authenticate, so a skipped step fails closed rather than leaving a
-- guessable one — which is what the committed literal was (ADR-035, audit #5).
CREATE ROLE orazaka_knowledge LOGIN;
CREATE DATABASE orazaka_knowledge_db OWNER orazaka_knowledge;
\c orazaka_knowledge_db

-- pgvector is NOT a 'trusted' extension, so CREATE EXTENSION requires a superuser.
-- Create it as the bootstrap superuser FIRST, then drop to the service role for the
-- tables it owns — the role uses the vector type freely (public-schema usage).
CREATE EXTENSION IF NOT EXISTS vector;

SET ROLE orazaka_knowledge;

-- Consumer-side idempotency (AGENTS.md §6) — this service's own dedup ledger.
CREATE TABLE processed_messages (
    consumer VARCHAR(100) NOT NULL,
    message_id VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (consumer, message_id)
);

CREATE TABLE orazaka_tools_rag_source (
    id SERIAL PRIMARY KEY,
    tool_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    metadata TEXT,
    ingested BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    user_id VARCHAR(255),
    CONSTRAINT unique_tool_content UNIQUE (tool_id, content)
);
CREATE INDEX idx_tools_rag_source_user ON orazaka_tools_rag_source(user_id);

-- ============================================================================
-- KNOWLEDGE SEED DATA
-- ============================================================================

