package com.travelai.domain.user;

import com.travelai.domain.auth.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entitat que representa la relació de seguiment entre usuaris.
 * Clau primària composta: (follower_id, following_id).
 */
@Entity
@Table(name = "follows", indexes = {
        @Index(name = "idx_follows_follower",  columnList = "follower_id"),
        @Index(name = "idx_follows_following", columnList = "following_id")
})
@IdClass(FollowId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Follow {

    @Id
    @Column(name = "follower_id", nullable = false)
    private UUID followerId;

    @Id
    @Column(name = "following_id", nullable = false)
    private UUID followingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", insertable = false, updatable = false)
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", insertable = false, updatable = false)
    private User following;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
