package com.orazaka.knowledgeservice.infrastructure.adapter.rest.dto;

/**
 * Request for a tool RAG-source substring search.
 *
 * @param toolId the tool whose sources to search
 * @param userId the requesting user's opaque id (may be null for platform-wide access)
 * @param query the search text; blank returns every visible source for the tool
 */
public record SourceSearchRequest(String toolId, String userId, String query) {}
