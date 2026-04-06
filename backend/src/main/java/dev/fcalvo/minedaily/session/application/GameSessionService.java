package dev.fcalvo.minedaily.session.application;

import dev.fcalvo.minedaily.challenge.application.ChallengeQueryService;
import dev.fcalvo.minedaily.challenge.domain.DailyChallenge;
import dev.fcalvo.minedaily.session.domain.GameSession;
import dev.fcalvo.minedaily.session.domain.GameSessionStatus;
import dev.fcalvo.minedaily.session.infrastructure.GameSessionRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
/**
 * Encapsulates the minimal session lifecycle for slice 2:
 * create or reuse the active session for the current challenge,
 * retrieve the active session, and enforce session ownership.
 */
public class GameSessionService {

	private final GameSessionRepository gameSessionRepository;
	private final ChallengeQueryService challengeQueryService;
	private final Clock businessClock;

	public GameSessionService(
		GameSessionRepository gameSessionRepository,
		ChallengeQueryService challengeQueryService,
		Clock businessClock
	) {
		this.gameSessionRepository = gameSessionRepository;
		this.challengeQueryService = challengeQueryService;
		this.businessClock = businessClock;
	}

	@Transactional
	public CreateSessionResult createOrReuseCurrentSession(String userId) {
		DailyChallenge currentChallenge = challengeQueryService.getCurrentChallenge();
		Optional<GameSession> existingSession = findActiveSession(userId, currentChallenge);
		if (existingSession.isPresent()) {
			return new CreateSessionResult(existingSession.get(), false);
		}

		GameSession newSession = GameSession.create(
			generateSessionId(),
			userId,
			currentChallenge,
			OffsetDateTime.now(businessClock)
		);

		try {
			return new CreateSessionResult(gameSessionRepository.saveAndFlush(newSession), true);
		} catch (DataIntegrityViolationException exception) {
			// If two requests race, the database-level unique active key keeps only one winner.
			GameSession reusedSession = findActiveSession(userId, currentChallenge)
				.orElseThrow(() -> exception);
			return new CreateSessionResult(reusedSession, false);
		}
	}

	@Transactional(readOnly = true)
	public Optional<GameSession> getActiveCurrentSession(String userId) {
		DailyChallenge currentChallenge = challengeQueryService.getCurrentChallenge();
		return findActiveSession(userId, currentChallenge);
	}

	@Transactional(readOnly = true)
	public GameSession getOwnedSession(String sessionId, String userId) {
		GameSession session = gameSessionRepository.findBySessionId(sessionId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

		if (!session.getUserId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Session does not belong to the authenticated user");
		}

		return session;
	}

	private Optional<GameSession> findActiveSession(String userId, DailyChallenge challenge) {
		return gameSessionRepository.findByUserIdAndChallengeChallengeIdAndStatus(
			userId,
			challenge.getChallengeId(),
			GameSessionStatus.IN_PROGRESS
		);
	}

	private String generateSessionId() {
		return "ses_" + UUID.randomUUID().toString().replace("-", "");
	}

}
