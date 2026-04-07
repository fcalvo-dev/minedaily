package dev.fcalvo.minedaily.challenge.application;

import dev.fcalvo.minedaily.challenge.domain.DailyChallenge;
import dev.fcalvo.minedaily.session.domain.GameSession;
import dev.fcalvo.minedaily.session.domain.GameSessionStatus;
import dev.fcalvo.minedaily.session.infrastructure.GameSessionRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentChallengeLeaderboardService {

	private static final int MAX_ENTRIES = 50;

	private final ChallengeQueryService challengeQueryService;
	private final GameSessionRepository gameSessionRepository;

	public CurrentChallengeLeaderboardService(
		ChallengeQueryService challengeQueryService,
		GameSessionRepository gameSessionRepository
	) {
		this.challengeQueryService = challengeQueryService;
		this.gameSessionRepository = gameSessionRepository;
	}

	@Transactional(readOnly = true)
	public CurrentChallengeLeaderboard getCurrentLeaderboard() {
		DailyChallenge currentChallenge = challengeQueryService.getCurrentChallenge();
		Map<String, GameSession> firstSessionByUser = new LinkedHashMap<>();
		gameSessionRepository
			.findByChallengeChallengeIdOrderByUserIdAscStartedAtAscSessionIdAsc(currentChallenge.getChallengeId())
			.forEach(session -> firstSessionByUser.putIfAbsent(session.getUserId(), session));

		List<GameSession> rankedSessions = firstSessionByUser.values().stream()
			.filter(this::qualifiesForLeaderboard)
			.sorted(leaderboardOrder())
			.limit(MAX_ENTRIES)
			.toList();

		List<CurrentChallengeLeaderboard.Entry> entries = new ArrayList<>(rankedSessions.size());
		for (int index = 0; index < rankedSessions.size(); index++) {
			GameSession session = rankedSessions.get(index);
			entries.add(new CurrentChallengeLeaderboard.Entry(
				index + 1,
				session.getUserId(),
				durationMs(session),
				session.getErrorCount(),
				session.getClickCount(),
				session.getRemainingLives(),
				session.getEndedAt()
			));
		}

		return new CurrentChallengeLeaderboard(
			currentChallenge.getChallengeId(),
			currentChallenge.getChallengeDate(),
			List.copyOf(entries)
		);
	}

	private boolean qualifiesForLeaderboard(GameSession session) {
		return session.getStatus() == GameSessionStatus.WON && session.getEndedAt() != null;
	}

	private Comparator<GameSession> leaderboardOrder() {
		return Comparator
			.comparingLong(this::durationMs)
			.thenComparingInt(GameSession::getErrorCount)
			.thenComparingInt(GameSession::getClickCount)
			.thenComparing(GameSession::getEndedAt, Comparator.nullsLast(OffsetDateTime::compareTo))
			.thenComparing(GameSession::getSessionId);
	}

	private long durationMs(GameSession session) {
		return Duration.between(
			Objects.requireNonNull(session.getStartedAt(), "startedAt is required"),
			Objects.requireNonNull(session.getEndedAt(), "endedAt is required")
		).toMillis();
	}

}
