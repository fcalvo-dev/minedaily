package dev.fcalvo.minedaily.session.application;

import dev.fcalvo.minedaily.challenge.application.ChallengeQueryService;
import dev.fcalvo.minedaily.challenge.domain.DailyChallenge;
import dev.fcalvo.minedaily.session.domain.GameSession;
import dev.fcalvo.minedaily.session.domain.GameSessionStatus;
import dev.fcalvo.minedaily.session.infrastructure.GameSessionRepository;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserChallengeStatusService {

	private final ChallengeQueryService challengeQueryService;
	private final GameSessionRepository gameSessionRepository;

	public CurrentUserChallengeStatusService(
		ChallengeQueryService challengeQueryService,
		GameSessionRepository gameSessionRepository
	) {
		this.challengeQueryService = challengeQueryService;
		this.gameSessionRepository = gameSessionRepository;
	}

	@Transactional(readOnly = true)
	public CurrentUserChallengeStatus getCurrentUserChallengeStatus(String userId) {
		DailyChallenge currentChallenge = challengeQueryService.getCurrentChallenge();
		Optional<GameSession> activeSession = gameSessionRepository.findByUserIdAndChallengeChallengeIdAndStatus(
			userId,
			currentChallenge.getChallengeId(),
			GameSessionStatus.IN_PROGRESS
		);
		Optional<GameSession> firstPersistedSession = gameSessionRepository
			.findFirstByUserIdAndChallengeChallengeIdOrderByStartedAtAscSessionIdAsc(
				userId,
				currentChallenge.getChallengeId()
			);

		// Status is derived only from persisted sessions: an active session takes precedence;
		// otherwise the first persisted session stands in for the first eligible attempt.
		if (activeSession.isPresent()) {
			GameSession relevantSession = activeSession.get();
			GameSession firstSession = firstPersistedSession.orElse(relevantSession);
			return fromSession(currentChallenge, relevantSession, firstSession);
		}

		return firstPersistedSession
			.map(session -> fromSession(currentChallenge, session, session))
			.orElseGet(() -> notPlayed(currentChallenge));
	}

	private CurrentUserChallengeStatus notPlayed(DailyChallenge challenge) {
		return new CurrentUserChallengeStatus(
			challenge.getChallengeId(),
			challenge.getChallengeDate(),
			CurrentUserChallengeStatus.Status.NOT_PLAYED,
			false,
			false,
			null,
			true,
			false,
			false,
			null,
			null,
			null,
			null
		);
	}

	private CurrentUserChallengeStatus fromSession(
		DailyChallenge challenge,
		GameSession relevantSession,
		GameSession firstSession
	) {
		CurrentUserChallengeStatus.Status status = toStatus(relevantSession.getStatus());
		boolean hasActiveSession = relevantSession.getStatus() == GameSessionStatus.IN_PROGRESS;
		boolean isFirstPersistedSession = relevantSession.getSessionId().equals(firstSession.getSessionId());
		// The current schema does not persist attemptNumber/isLeaderboardEligible yet.
		// Until then, eligibility is derived from the first persisted session for the challenge.
		boolean leaderboardEligible = isFirstPersistedSession
			&& relevantSession.getStatus() != GameSessionStatus.LOST;

		return new CurrentUserChallengeStatus(
			challenge.getChallengeId(),
			challenge.getChallengeDate(),
			status,
			true,
			hasActiveSession,
			hasActiveSession ? relevantSession.getSessionId() : null,
			status == CurrentUserChallengeStatus.Status.NOT_PLAYED,
			status == CurrentUserChallengeStatus.Status.IN_PROGRESS,
			leaderboardEligible,
			relevantSession.getRemainingLives(),
			relevantSession.getMaxLives(),
			toFinishedOutcome(relevantSession.getStatus()),
			relevantSession.getSessionId()
		);
	}

	private CurrentUserChallengeStatus.Status toStatus(GameSessionStatus status) {
		return switch (status) {
			case IN_PROGRESS -> CurrentUserChallengeStatus.Status.IN_PROGRESS;
			case WON -> CurrentUserChallengeStatus.Status.WON;
			case LOST -> CurrentUserChallengeStatus.Status.LOST;
		};
	}

	private CurrentUserChallengeStatus.FinishedOutcome toFinishedOutcome(GameSessionStatus status) {
		return switch (status) {
			case IN_PROGRESS -> null;
			case WON -> CurrentUserChallengeStatus.FinishedOutcome.WON;
			case LOST -> CurrentUserChallengeStatus.FinishedOutcome.LOST;
		};
	}

}
