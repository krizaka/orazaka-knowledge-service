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
-- RECOMMENDATIONS (ADR-078) — a two-stage recommender served to a product's
-- service account. Every row carries tenant_id (the account's user id, an opaque
-- ActorId); users are the product's PSEUDONYMS, items and creators its public ids.
-- Titles and descriptions are embedded, never stored. Kept identical to the
-- incremental script infra/migrations/2026-10-15-recommendations.sql (workspace).
-- ============================================================================
CREATE TYPE reco_event_kind AS ENUM
    ('VIEW', 'LIKE', 'SAVE', 'SHARE', 'TIP', 'UNLOCK', 'FOLLOW', 'UNFOLLOW', 'SKIP', 'REPORT');

-- The parameters, versioned: a new version is a new row with its reason; the newest
-- row of the tenant wins, else the newest of '*' (every tenant).
CREATE TABLE reco_settings (
    tenant_id VARCHAR(64) NOT NULL,
    version INTEGER NOT NULL CHECK (version >= 1),
    settings JSONB NOT NULL,
    reason TEXT NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, version)
);

CREATE TABLE reco_item (
    tenant_id VARCHAR(64) NOT NULL,
    item_id VARCHAR(64) NOT NULL,
    creator_id VARCHAR(64) NOT NULL,
    tags TEXT[] NOT NULL DEFAULT '{}',
    language VARCHAR(35),
    duration_seconds INTEGER,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL,
    content_hash CHAR(64) NOT NULL,
    model VARCHAR(120),
    embedding vector(1024),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, item_id)
);
CREATE INDEX reco_item_embedding_idx ON reco_item USING hnsw (embedding vector_cosine_ops);
CREATE INDEX reco_item_published_idx ON reco_item (tenant_id, published_at DESC);
CREATE INDEX reco_item_creator_idx ON reco_item (tenant_id, creator_id);

CREATE TABLE reco_event (
    tenant_id VARCHAR(64) NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    item_id VARCHAR(64),
    creator_id VARCHAR(64),
    kind reco_event_kind NOT NULL,
    watched_pct SMALLINT CHECK (watched_pct BETWEEN 0 AND 100),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, event_id),
    CHECK (item_id IS NOT NULL OR creator_id IS NOT NULL)
);
CREATE INDEX reco_event_user_idx ON reco_event (tenant_id, user_id, occurred_at DESC);
CREATE INDEX reco_event_item_idx ON reco_event (tenant_id, item_id);
CREATE INDEX reco_event_time_idx ON reco_event (tenant_id, occurred_at);

CREATE TABLE reco_user_profile (
    tenant_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    embedding vector(1024) NOT NULL,
    interactions INTEGER NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (tenant_id, user_id)
);

-- What was served, for measurement (CTR, offline evaluation) — ids only, purged after
-- the tenant's requestLogDays.
CREATE TABLE reco_request (
    request_id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(128),
    kind VARCHAR(16) NOT NULL,
    strategy VARCHAR(16) NOT NULL,
    item_ids TEXT[] NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX reco_request_time_idx ON reco_request (tenant_id, created_at);

-- Version 1 of the parameters, for every tenant (ADR-078 §5).
INSERT INTO reco_settings (tenant_id, version, settings, reason, created_by) VALUES ('*', 1, '{
  "enabled": true,
  "weights": {"similarity": 0.45, "coEngagement": 0.2, "freshness": 0.15, "followed": 0.12, "popularity": 0.08},
  "kindWeights": {"VIEW": 1, "LIKE": 3, "SAVE": 3, "SHARE": 2, "TIP": 5, "UNLOCK": 5,
                  "FOLLOW": 0, "UNFOLLOW": 0, "SKIP": -2, "REPORT": -5},
  "halfLifeHours": 168, "freshHalfLifeHours": 72, "mmrLambda": 0.7, "creatorCap": 2,
  "candidatePool": 100, "profileEvents": 200, "popularWindowDays": 7, "excludeSeen": true,
  "retentionDays": 180, "requestLogDays": 30, "minAudience": 3
}', 'ADR-078: the first transparent weights', 'migration');

-- ============================================================================
-- KNOWLEDGE SEED DATA
-- ============================================================================

