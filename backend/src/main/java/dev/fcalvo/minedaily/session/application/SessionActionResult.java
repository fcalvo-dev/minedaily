package dev.fcalvo.minedaily.session.application;

import dev.fcalvo.minedaily.session.domain.GameSession;

public record SessionActionResult(
	String actionType,
	int row,
	int col,
	String result,
	GameSession session
) {
}
