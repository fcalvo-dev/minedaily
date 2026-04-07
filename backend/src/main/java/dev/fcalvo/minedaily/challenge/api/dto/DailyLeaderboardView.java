package dev.fcalvo.minedaily.challenge.api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record DailyLeaderboardView(
	String challengeId,
	LocalDate challengeDate,
	List<LeaderboardEntryView> entries
) {

	public record LeaderboardEntryView(
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
