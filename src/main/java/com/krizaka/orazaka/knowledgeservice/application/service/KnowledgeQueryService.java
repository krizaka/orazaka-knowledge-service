package com.krizaka.orazaka.knowledgeservice.application.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * The knowledge service's read surface: semantic {@link #retrieveContext} over the vector store and
 * substring {@link #searchSources} over the tool RAG-source table. Both degrade to an empty string
 * when nothing is available — an unreachable vector store or no matching rows must never fail the
 * caller (RAG enrichment and tool lookup are additive).
 *
 * <p>Vector retrieval is dormant until an embedding-model-backed {@link VectorStore} bean exists;
 * {@link #retrieveContext} then returns {@code ""}. Source search is the live path today.
 */
@Service
public class KnowledgeQueryService {

  private final VectorStore vectorStore;
  private final JdbcTemplate jdbcTemplate;

  public KnowledgeQueryService(
      ObjectProvider<VectorStore> vectorStoreProvider, JdbcTemplate jdbcTemplate) {
    this.vectorStore = vectorStoreProvider.getIfAvailable();
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate cannot be null");
  }

  /**
   * Retrieves relevant context for a query from the vector store, or {@code ""} when unavailable.
   */
  public String retrieveContext(String query, int topK) {
    if (vectorStore == null || query == null || query.isBlank()) {
      return "";
    }
    List<Document> documents =
        vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(topK).build());
    return documents.stream().map(Document::getText).collect(Collectors.joining("\n---\n"));
  }

  /**
   * Substring-searches the tool RAG sources visible to the requesting user (platform-wide rows have
   * a null user), scoped to a tool. A blank query returns every visible source for the tool.
   *
   * @return matching sources' content joined by blank lines, or {@code ""} when none match
   */
  public String searchSources(String toolId, String userId, String query) {
    if (toolId == null || toolId.isBlank()) {
      return "";
    }
    List<String> contents =
        jdbcTemplate.query(
            "SELECT content FROM orazaka_tools_rag_source "
                + "WHERE tool_id = ? "
                + "AND (user_id IS NULL OR user_id = ?) "
                + "AND (? = '' OR LOWER(content) LIKE LOWER('%' || ? || '%'))",
            (rs, rowNum) -> rs.getString("content"),
            toolId,
            userId,
            query == null ? "" : query,
            query == null ? "" : query);
    return String.join("\n\n", contents);
  }
}
