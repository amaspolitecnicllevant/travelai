package com.travelai.domain.trip.dto;

import com.travelai.domain.trip.Comment;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
    UUID    id,
    String  content,
    String  authorUsername,
    String  authorName,
    String  authorAvatarUrl,
    Instant createdAt
) {
    public static CommentResponse from(Comment c) {
        return new CommentResponse(
            c.getId(),
            c.getContent(),
            c.getAuthor().getUsername(),
            c.getAuthor().getName(),
            c.getAuthor().getAvatarUrl(),
            c.getCreatedAt()
        );
    }
}
