package dev.fcalvo.minedaily.session.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.fcalvo.minedaily.session.application.CurrentUserChallengeStatus;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CurrentUserChallengeStatusView(
	String challengeId,
	LocalDate challengeDate,
	CurrentUserChallengeStatus.Status status,
	boolean hasPlayedCurrentChallenge,
	boolean hasActiveSession,
	String activeSessionId,
	boolean canStartSession,
	boolean canResumeSession,
	boolean leaderboardEligible,
	Integer remainingLives,
	Integer maxLives,
	CurrentUserChallengeStatus.FinishedOutcome finishedOutcome,
	String sessionId
) {
}
