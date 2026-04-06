package dev.fcalvo.minedaily.challenge.api.dto;

import java.time.LocalDate;

public record ProvisionChallengeResponse(
	String challengeId,
	LocalDate challengeDate,
	boolean created
) {
}
