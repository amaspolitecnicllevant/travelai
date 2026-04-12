package com.travelai.domain.trip;

import com.travelai.domain.auth.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_comments_trip_id",   columnList = "trip_id"),
    @Index(name = "idx_comments_author_id", columnList = "author_id"),
})
@Getter @Setter @NoArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Comment(Trip trip, User author, String content) {
        this.trip    = trip;
        this.author  = author;
        this.content = content;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
