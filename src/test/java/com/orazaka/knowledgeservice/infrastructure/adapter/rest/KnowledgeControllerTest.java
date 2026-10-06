package com.orazaka.knowledgeservice.infrastructure.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.orazaka.knowledgeservice.application.service.KnowledgeQueryService;
import com.orazaka.knowledgeservice.infrastructure.adapter.rest.dto.ContentResponse;
import com.orazaka.knowledgeservice.infrastructure.adapter.rest.dto.RetrieveRequest;
import com.orazaka.knowledgeservice.infrastructure.adapter.rest.dto.SourceSearchRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class KnowledgeControllerTest {

  private final KnowledgeQueryService queryService = mock(KnowledgeQueryService.class);
  private final KnowledgeController controller = new KnowledgeController(queryService);

  @Test
  @DisplayName("retrieve returns the resolved context (defaulting topK)")
  void retrieveReturnsContext() {
    when(queryService.retrieveContext("q", 3)).thenReturn("ctx");

    ResponseEntity<ContentResponse> response = controller.retrieve(new RetrieveRequest("q", null));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().content()).isEqualTo("ctx");
  }

  @Test
  @DisplayName("sources/search returns the joined source content")
  void searchReturnsSources() {
    when(queryService.searchSources("searchWeb", "u", "q")).thenReturn("joined");

    ResponseEntity<ContentResponse> response =
        controller.searchSources(new SourceSearchRequest("searchWeb", "u", "q"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().content()).isEqualTo("joined");
  }
}
