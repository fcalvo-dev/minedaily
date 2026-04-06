package dev.fcalvo.minedaily.challenge.api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CurrentChallengeView(
	String challengeId,
	LocalDate challengeDate,
	String timezone,
	OffsetDateTime windowStartAt,
	OffsetDateTime windowEndAt,
	OffsetDateTime rolloverAt,
	BoardView board
) {

	public record BoardView(int rows, int cols, int mineCount) {
	}

}
