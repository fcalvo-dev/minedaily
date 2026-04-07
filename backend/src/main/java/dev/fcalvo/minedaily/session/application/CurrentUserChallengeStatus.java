package dev.fcalvo.minedaily.session.application;

import java.time.LocalDate;

public record CurrentUserChallengeStatus(
	String challengeId,
	LocalDate challengeDate,
	Status status,
	boolean hasPlayedCurrentChallenge,
	boolean hasActiveSession,
	String activeSessionId,
	boolean canStartSession,
	boolean canResumeSession,
	boolean leaderboardEligible,
	Integer remainingLives,
	Integer maxLives,
	FinishedOutcome finishedOutcome,
	String sessionId
) {

	public enum Status {
		NOT_PLAYED,
		IN_PROGRESS,
		WON,
		LOST
	}

	public enum FinishedOutcome {
		WON,
		LOST
	}

}
