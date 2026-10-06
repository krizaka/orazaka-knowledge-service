package com.orazaka.knowledgeservice.application.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Async RAG indexing: loads a tool's not-yet-ingested sources into the vector store and marks them
 * ingested. Dormant until an embedding-model-backed {@link VectorStore} bean exists — with none, a
 * request is a logged no-op (honest skip, never a fake success): there is nothing to embed into.
 */
@Service
public class IngestionService {

  private static final Logger logger = LoggerFactory.getLogger(IngestionService.class);

  private final VectorStore vectorStore;
  private final JdbcTemplate jdbcTemplate;

  public IngestionService(
      ObjectProvider<VectorStore> vectorStoreProvider, JdbcTemplate jdbcTemplate) {
    this.vectorStore = vectorStoreProvider.getIfAvailable();
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate cannot be null");
  }

  /**
   * Ingests a tool's pending RAG sources into the vector store.
   *
   * @param toolId the tool whose sources to index
   */
  public void ingest(String toolId) {
    if (vectorStore == null) {
      logger.info(
          "Vector store unavailable (no embedding model) — skipping RAG ingestion for {}", toolId);
      return;
    }
    if (toolId == null || toolId.isBlank()) {
      return;
    }
    List<Source> pending =
        jdbcTemplate.query(
            "SELECT id, content FROM orazaka_tools_rag_source "
                + "WHERE tool_id = ? AND ingested = FALSE",
            (rs, rowNum) -> new Source(rs.getLong("id"), rs.getString("content")),
            toolId);
    for (Source source : pending) {
      vectorStore.add(List.of(new Document(source.content(), Map.of("tool_id", toolId))));
      jdbcTemplate.update(
          "UPDATE orazaka_tools_rag_source SET ingested = TRUE WHERE id = ?", source.id());
    }
    logger.info("Ingested {} RAG source(s) for tool {}", pending.size(), toolId);
  }

  private record Source(long id, String content) {}
}
