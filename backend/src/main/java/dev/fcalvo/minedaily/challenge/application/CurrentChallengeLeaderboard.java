package dev.fcalvo.minedaily.challenge.application;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record CurrentChallengeLeaderboard(
	String challengeId,
	LocalDate challengeDate,
	List<Entry> entries
) {

	public record Entry(
		int position,
		String displayName,
		long durationMs,
		int errorCount,
		int clickCount,
		int remainingLives,
		OffsetDateTime endedAt
	) {
	}

}
