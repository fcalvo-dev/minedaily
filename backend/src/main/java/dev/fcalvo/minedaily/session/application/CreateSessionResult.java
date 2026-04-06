package dev.fcalvo.minedaily.session.application;

import dev.fcalvo.minedaily.session.domain.GameSession;

/**
 * Distinguishes between creating a fresh session and reusing an existing active one.
 */
public record CreateSessionResult(GameSession session, boolean created) {
}
