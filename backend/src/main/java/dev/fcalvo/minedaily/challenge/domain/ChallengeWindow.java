package dev.fcalvo.minedaily.challenge.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ChallengeWindow(
	LocalDate challengeDate,
	OffsetDateTime windowStartAt,
	OffsetDateTime windowEndAt,
	OffsetDateTime rolloverAt,
	String timezone
) {
}
