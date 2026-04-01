package com.travelai.domain.legal.dto;

import com.travelai.domain.legal.DataDeletionRequest.DeletionStatus;

import java.time.Instant;

/**
 * Resposta per a GET /api/my-data/deletion-status
 * i POST /api/my-data/delete.
 */
public record DeletionRequestResponse(
    java.util.UUID id,
    Instant requestedAt,
    Instant scheduledPurgeAt,
    DeletionStatus status
) {}
