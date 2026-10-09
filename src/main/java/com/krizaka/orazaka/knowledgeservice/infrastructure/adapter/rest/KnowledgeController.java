package com.krizaka.orazaka.knowledgeservice.infrastructure.adapter.rest;

import com.krizaka.orazaka.knowledgeservice.application.service.KnowledgeQueryService;
import com.krizaka.orazaka.knowledgeservice.infrastructure.adapter.rest.dto.ContentResponse;
import com.krizaka.orazaka.knowledgeservice.infrastructure.adapter.rest.dto.RetrieveRequest;
import com.krizaka.orazaka.knowledgeservice.infrastructure.adapter.rest.dto.SourceSearchRequest;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal knowledge read surface (Phase 4). Consumed by the core's {@code KnowledgeService} HTTP
 * adapter; internal-only (no security starter — never reachable through the edge). Both endpoints
 * return a uniform {@link ContentResponse} whose content is empty when nothing is available.
 */
@RestController
@RequestMapping("/internal/v1/knowledge")
public class KnowledgeController {

  private final KnowledgeQueryService knowledgeQueryService;

  public KnowledgeController(KnowledgeQueryService knowledgeQueryService) {
    this.knowledgeQueryService =
        Objects.requireNonNull(knowledgeQueryService, "KnowledgeQueryService required");
  }

  /** Semantic RAG retrieval over the vector store. */
  @PostMapping("/retrieve")
  public ResponseEntity<ContentResponse> retrieve(@RequestBody RetrieveRequest request) {
    String context = knowledgeQueryService.retrieveContext(request.query(), request.resolvedTopK());
    return ResponseEntity.ok(new ContentResponse(context));
  }

  /** Substring search over a tool's registered RAG sources. */
  @PostMapping("/sources/search")
  public ResponseEntity<ContentResponse> searchSources(@RequestBody SourceSearchRequest request) {
    String content =
        knowledgeQueryService.searchSources(request.toolId(), request.userId(), request.query());
    return ResponseEntity.ok(new ContentResponse(content));
  }
}
