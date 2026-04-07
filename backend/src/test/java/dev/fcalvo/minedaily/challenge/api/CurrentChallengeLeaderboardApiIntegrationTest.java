package dev.fcalvo.minedaily.challenge.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.fcalvo.minedaily.challenge.application.ChallengeQueryService;
import dev.fcalvo.minedaily.challenge.domain.DailyChallenge;
import dev.fcalvo.minedaily.challenge.infrastructure.DailyChallengeRepository;
import dev.fcalvo.minedaily.session.domain.GameSession;
import dev.fcalvo.minedaily.session.domain.GameSessionStatus;
import dev.fcalvo.minedaily.session.infrastructure.GameSessionRepository;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CurrentChallengeLeaderboardApiIntegrationTest {

	private static final String LEADERBOARD_PATH = "/api/challenges/current/leaderboard";
	private static final OffsetDateTime BASE_STARTED_AT = OffsetDateTime.parse("2026-04-07T12:00:00-03:00");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ChallengeQueryService challengeQueryService;

	@Autowired
	private DailyChallengeRepository dailyChallengeRepository;

	@Autowired
	private GameSessionRepository gameSessionRepository;

	private DailyChallenge currentChallenge;

	@BeforeEach
	void setUp() {
		gameSessionRepository.deleteAll();
		dailyChallengeRepository.deleteAll();
		currentChallenge = challengeQueryService.getCurrentChallenge();
	}

	@Test
	void leaderboardReturnsEmptyListWhenThereAreNoEligibleWonSessions() throws Exception {
		saveSession("ses_lost", "alice", GameSessionStatus.LOST, 1_000, 3, 10);
		saveSession("ses_in_progress", "bob", GameSessionStatus.IN_PROGRESS, 0, 0, 4);

		mockMvc.perform(get(LEADERBOARD_PATH))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.challengeId", is(currentChallenge.getChallengeId())))
			.andExpect(jsonPath("$.challengeDate").exists())
			.andExpect(jsonPath("$.entries", hasSize(0)));
	}

	@Test
	void leaderboardExcludesLostSessions() throws Exception {
		saveSession("ses_lost", "alice", GameSessionStatus.LOST, 1_000, 3, 10);
		saveSession("ses_won", "bob", GameSessionStatus.WON, 1_000, 0, 10);

		mockMvc.perform(get(LEADERBOARD_PATH))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.entries", hasSize(1)))
			.andExpect(jsonPath("$.entries[0].displayName", is("bob")));
	}

	@Test
	void leaderboardExcludesInProgressSessions() throws Exception {
		saveSession("ses_in_progress", "alice", GameSessionStatus.IN_PROGRESS, 0, 0, 10);
		saveSession("ses_won", "bob", GameSessionStatus.WON, 1_000, 0, 10);

		mockMvc.perform(get(LEADERBOARD_PATH))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.entries", hasSize(1)))
			.andExpect(jsonPath("$.entries[0].displayName", is("bob")));
	}

	@Test
	void leaderboardExcludesNonEligibleSessionsAfterTheFirstUserSession() throws Exception {
		saveSession("ses_alice_first_lost", "alice", GameSessionStatus.LOST, 1_000, 3, 10);
		saveSession(
			"ses_alice_second_won",
			"alice",
			GameSessionStatus.WON,
			BASE_STARTED_AT.plusSeconds(10),
			800,
			0,
			8
		);
		saveSession("ses_bob_won", "bob", GameSessionStatus.WON, 1_200, 0, 12);

		mockMvc.perform(get(LEADERBOARD_PATH))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.entries", hasSize(1)))
			.andExpect(jsonPath("$.entries[0].displayName", is("bob")));
	}

	@Test
	void leaderboardOrdersByShortestDuration() throws Exception {
		saveSession("ses_slow", "alice", GameSessionStatus.WON, 2_000, 0, 10);
		saveSession("ses_fast", "bob", GameSessionStatus.WON, 1_000, 0, 10);

		mockMvc.perform(get(LEADERBOARD_PATH))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.entries[*].displayName", contains("bob", "alice")));
	}

	@Test
	void leaderboardBreaksDurationTiesByErrorCount() throws Exception {
		saveSession("ses_more_errors", "alice", GameSessionStatus.WON, 1_000, 1, 10);
		saveSession("ses_fewer_errors", "bob", GameSessionStatus.WON, 1_000, 0, 10);

		mockMvc.perform(get(LEADERBOARD_PATH))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.entries[*].displayName", contains("bob", "alice")));
	}

	@Test
	void leaderboardBreaksErrorTiesByClickCount() throws Exception {
		saveSession("ses_more_clicks", "alice", GameSessionStatus.WON, 1_000, 0, 11);
		saveSession("ses_fewer_clicks", "bob", GameSessionStatus.WON, 1_000, 0, 10);

		mockMvc.perform(get(LEADERBOARD_PATH))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.entries[*].displayName", contains("bob", "alice")));
	}

	@Test
	void leaderboardBreaksClickTiesByEndedAt() throws Exception {
		saveSession("ses_later", "alice", GameSessionStatus.WON, BASE_STARTED_AT.plusSeconds(10), 1_000, 0, 10);
		saveSession("ses_earlier", "bob", GameSessionStatus.WON, BASE_STARTED_AT, 1_000, 0, 10);

		mockMvc.perform(get(LEADERBOARD_PATH))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.entries[*].displayName", contains("bob", "alice")));
	}

	@Test
	void leaderboardBreaksFinalTiesBySessionId() throws Exception {
		saveSession("ses_b", "alice", GameSessionStatus.WON, BASE_STARTED_AT, 1_000, 0, 10);
		saveSession("ses_a", "bob", GameSessionStatus.WON, BASE_STARTED_AT, 1_000, 0, 10);

		mockMvc.perform(get(LEADERBOARD_PATH))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.entries[*].displayName", contains("bob", "alice")))
			.andExpect(jsonPath("$.entries[*].position", contains(1, 2)));
	}

	@Test
	void leaderboardLimitsResponseToTopFiftyEntries() throws Exception {
		for (int index = 0; index < 55; index++) {
			saveSession(
				"ses_%02d".formatted(index),
				"user_%02d".formatted(index),
				GameSessionStatus.WON,
				1_000L + index,
				0,
				10
			);
		}

		mockMvc.perform(get(LEADERBOARD_PATH))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.entries", hasSize(50)))
			.andExpect(jsonPath("$.entries[0].displayName", is("user_00")))
			.andExpect(jsonPath("$.entries[49].displayName", is("user_49")));
	}

	private GameSession saveSession(
		String sessionId,
		String userId,
		GameSessionStatus status,
		long durationMs,
		int errorCount,
		int clickCount
	) {
		return saveSession(sessionId, userId, status, BASE_STARTED_AT, durationMs, errorCount, clickCount);
	}

	private GameSession saveSession(
		String sessionId,
		String userId,
		GameSessionStatus status,
		OffsetDateTime startedAt,
		long durationMs,
		int errorCount,
		int clickCount
	) {
		GameSession session = GameSession.create(sessionId, userId, currentChallenge, startedAt);
		session.applyGameplayUpdate(
			session.getBoardSnapshot(),
			remainingLives(status, errorCount),
			errorCount,
			clickCount,
			status,
			status == GameSessionStatus.IN_PROGRESS ? null : startedAt.plusNanos(durationMs * 1_000_000)
		);
		return gameSessionRepository.saveAndFlush(session);
	}

	private int remainingLives(GameSessionStatus status, int errorCount) {
		if (status == GameSessionStatus.LOST) {
			return 0;
		}
		return 3 - errorCount;
	}

}
