package com.krizaka.orazaka.knowledgeservice.recommendation.domain.model;

/**
 * What a batch of events did.
 *
 * @param accepted new events
 * @param duplicates events already known (same id) — at-least-once delivery is expected
 */
public record IngestResult(int accepted, int duplicates) {}
