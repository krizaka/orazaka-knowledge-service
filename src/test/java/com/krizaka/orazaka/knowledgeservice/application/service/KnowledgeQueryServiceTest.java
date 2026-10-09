package com.krizaka.orazaka.knowledgeservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class KnowledgeQueryServiceTest {

  @SuppressWarnings("unchecked")
  private static ObjectProvider<VectorStore> provider(VectorStore store) {
    ObjectProvider<VectorStore> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(store);
    return provider;
  }

  @Test
  @DisplayName("retrieveContext returns empty when no vector store is wired (dormant)")
  void retrieveDegradesWithoutVectorStore() {
    var service = new KnowledgeQueryService(provider(null), mock(JdbcTemplate.class));
    assertThat(service.retrieveContext("anything", 3)).isEmpty();
  }

  @Test
  @DisplayName("retrieveContext joins document text when a vector store is present")
  void retrieveJoinsDocuments() {
    VectorStore store = mock(VectorStore.class);
    when(store.similaritySearch(any(SearchRequest.class)))
        .thenReturn(List.of(new Document("alpha"), new Document("beta")));
    var service = new KnowledgeQueryService(provider(store), mock(JdbcTemplate.class));

    assertThat(service.retrieveContext("q", 2)).isEqualTo("alpha\n---\nbeta");
  }

  @Test
  @DisplayName("searchSources joins matching rows and is empty for a blank tool id")
  void searchSourcesJoinsRows() {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    when(jdbc.query(anyString(), any(RowMapper.class), any(), any(), any(), any()))
        .thenReturn(List.of("one", "two"));
    var service = new KnowledgeQueryService(provider(null), jdbc);

    assertThat(service.searchSources("searchWeb", "user-1", "q")).isEqualTo("one\n\ntwo");
    assertThat(service.searchSources("  ", "user-1", "q")).isEmpty();
  }
}
