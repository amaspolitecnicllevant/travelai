package com.travelai.domain.user;

import java.io.Serializable;
import java.util.UUID;

/**
 * Clau composta per a la taula follows.
 */
public record FollowId(UUID followerId, UUID followingId) implements Serializable {}
