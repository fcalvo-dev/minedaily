package dev.fcalvo.minedaily.session.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.fcalvo.minedaily.challenge.domain.ChallengeBoard;
import dev.fcalvo.minedaily.challenge.domain.ChallengeBoardReconstructor;
import dev.fcalvo.minedaily.challenge.infrastructure.DailyChallengeRepository;
import dev.fcalvo.minedaily.session.domain.CellState;
import dev.fcalvo.minedaily.session.domain.GameSession;
import dev.fcalvo.minedaily.session.domain.GameSessionStatus;
import dev.fcalvo.minedaily.session.infrastructure.GameSessionRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class GameSessionApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private GameSessionRepository gameSessionRepository;

	@Autowired
	private DailyChallengeRepository dailyChallengeRepository;

	@Autowired
	private ChallengeBoardReconstructor challengeBoardReconstructor;

	@BeforeEach
	void setUp() {
		gameSessionRepository.deleteAll();
		dailyChallengeRepository.deleteAll();
	}

	@Test
	void createCurrentSessionInitializesTheLifecycleState() throws Exception {
		mockMvc.perform(post("/api/challenges/current/sessions").with(user("alice")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.sessionId").exists())
			.andExpect(jsonPath("$.challengeId").exists())
			.andExpect(jsonPath("$.status", is("IN_PROGRESS")))
			.andExpect(jsonPath("$.lives.maxLives", is(3)))
			.andExpect(jsonPath("$.lives.remainingLives", is(3)))
			.andExpect(jsonPath("$.performance.errorCount", is(0)))
			.andExpect(jsonPath("$.performance.clickCount", is(0)))
			.andExpect(jsonPath("$.board.rows", is(10)))
			.andExpect(jsonPath("$.board.cols", is(10)))
			.andExpect(jsonPath("$.board.cells[0][0].state", is("HIDDEN")))
			.andExpect(jsonPath("$.board.cells[9][9].state", is("HIDDEN")));

		assertThat(gameSessionRepository.count()).isEqualTo(1);
		assertThat(dailyChallengeRepository.count()).isEqualTo(1);
	}

	@Test
	void createCurrentSessionIsIdempotentWhileTheSessionIsStillInProgress() throws Exception {
		MvcResult firstResponse = mockMvc.perform(post("/api/challenges/current/sessions").with(user("alice")))
			.andExpect(status().isCreated())
			.andReturn();

		String sessionId = JsonTestUtils.readString(firstResponse, "$.sessionId");

		mockMvc.perform(post("/api/challenges/current/sessions").with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sessionId", is(sessionId)));

		assertThat(gameSessionRepository.count()).isEqualTo(1);
	}

	@Test
	void activeCurrentSessionReturnsNoContentWhenTheUserHasNoSession() throws Exception {
		mockMvc.perform(get("/api/challenges/current/sessions/active").with(user("alice")))
			.andExpect(status().isNoContent());
	}

	@Test
	void activeCurrentSessionReturnsTheExistingSessionForTheCurrentChallenge() throws Exception {
		MvcResult createdSession = mockMvc.perform(post("/api/challenges/current/sessions").with(user("alice")))
			.andExpect(status().isCreated())
			.andReturn();

		String sessionId = JsonTestUtils.readString(createdSession, "$.sessionId");

		mockMvc.perform(get("/api/challenges/current/sessions/active").with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sessionId", is(sessionId)))
			.andExpect(jsonPath("$.status", is("IN_PROGRESS")));
	}

	@Test
	void currentChallengeStatusRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/challenges/current/status"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void currentChallengeStatusReturnsNotPlayedWhenTheUserHasNoSession() throws Exception {
		mockMvc.perform(get("/api/challenges/current/status").with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.challengeId").exists())
			.andExpect(jsonPath("$.challengeDate").exists())
			.andExpect(jsonPath("$.status", is("NOT_PLAYED")))
			.andExpect(jsonPath("$.hasPlayedCurrentChallenge", is(false)))
			.andExpect(jsonPath("$.hasActiveSession", is(false)))
			.andExpect(jsonPath("$.canStartSession", is(true)))
			.andExpect(jsonPath("$.canResumeSession", is(false)))
			.andExpect(jsonPath("$.leaderboardEligible", is(false)))
			.andExpect(jsonPath("$.activeSessionId").doesNotExist())
			.andExpect(jsonPath("$.sessionId").doesNotExist())
			.andExpect(jsonPath("$.remainingLives").doesNotExist())
			.andExpect(jsonPath("$.maxLives").doesNotExist())
			.andExpect(jsonPath("$.finishedOutcome").doesNotExist());
	}

	@Test
	void currentChallengeStatusReturnsInProgressWhenTheUserHasAnActiveSession() throws Exception {
		String sessionId = createSession("alice");

		mockMvc.perform(get("/api/challenges/current/status").with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("IN_PROGRESS")))
			.andExpect(jsonPath("$.hasPlayedCurrentChallenge", is(true)))
			.andExpect(jsonPath("$.hasActiveSession", is(true)))
			.andExpect(jsonPath("$.activeSessionId", is(sessionId)))
			.andExpect(jsonPath("$.sessionId", is(sessionId)))
			.andExpect(jsonPath("$.canStartSession", is(false)))
			.andExpect(jsonPath("$.canResumeSession", is(true)))
			.andExpect(jsonPath("$.leaderboardEligible", is(true)))
			.andExpect(jsonPath("$.remainingLives", is(3)))
			.andExpect(jsonPath("$.maxLives", is(3)))
			.andExpect(jsonPath("$.finishedOutcome").doesNotExist());
	}

	@Test
	void currentChallengeStatusReturnsWonWhenTheRelevantSessionWasWon() throws Exception {
		String sessionId = createWonSession("alice");

		mockMvc.perform(get("/api/challenges/current/status").with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("WON")))
			.andExpect(jsonPath("$.hasPlayedCurrentChallenge", is(true)))
			.andExpect(jsonPath("$.hasActiveSession", is(false)))
			.andExpect(jsonPath("$.activeSessionId").doesNotExist())
			.andExpect(jsonPath("$.sessionId", is(sessionId)))
			.andExpect(jsonPath("$.canStartSession", is(false)))
			.andExpect(jsonPath("$.canResumeSession", is(false)))
			.andExpect(jsonPath("$.leaderboardEligible", is(true)))
			.andExpect(jsonPath("$.remainingLives", is(3)))
			.andExpect(jsonPath("$.maxLives", is(3)))
			.andExpect(jsonPath("$.finishedOutcome", is("WON")));
	}

	@Test
	void currentChallengeStatusReturnsLostWhenTheRelevantSessionWasLost() throws Exception {
		String sessionId = createLostSession("alice");

		mockMvc.perform(get("/api/challenges/current/status").with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("LOST")))
			.andExpect(jsonPath("$.hasPlayedCurrentChallenge", is(true)))
			.andExpect(jsonPath("$.hasActiveSession", is(false)))
			.andExpect(jsonPath("$.activeSessionId").doesNotExist())
			.andExpect(jsonPath("$.sessionId", is(sessionId)))
			.andExpect(jsonPath("$.canStartSession", is(false)))
			.andExpect(jsonPath("$.canResumeSession", is(false)))
			.andExpect(jsonPath("$.leaderboardEligible", is(false)))
			.andExpect(jsonPath("$.remainingLives", is(0)))
			.andExpect(jsonPath("$.maxLives", is(3)))
			.andExpect(jsonPath("$.finishedOutcome", is("LOST")));
	}

	@Test
	void currentChallengeStatusOnlyReturnsActiveSessionIdForActiveSessions() throws Exception {
		mockMvc.perform(get("/api/challenges/current/status").with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.activeSessionId").doesNotExist());

		String activeSessionId = createSession("alice");
		mockMvc.perform(get("/api/challenges/current/status").with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.activeSessionId", is(activeSessionId)));

		createLostSession("bob");
		mockMvc.perform(get("/api/challenges/current/status").with(user("bob")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.activeSessionId").doesNotExist());
	}

	@Test
	void currentChallengeStatusDerivesCanStartSessionOnlyForNotPlayed() throws Exception {
		mockMvc.perform(get("/api/challenges/current/status").with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.canStartSession", is(true)));

		createSession("alice");

		mockMvc.perform(get("/api/challenges/current/status").with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.canStartSession", is(false)));
	}

	@Test
	void currentChallengeStatusDerivesCanResumeSessionOnlyForInProgress() throws Exception {
		mockMvc.perform(get("/api/challenges/current/status").with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.canResumeSession", is(false)));

		String sessionId = createSession("alice");

		mockMvc.perform(get("/api/challenges/current/status").with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sessionId", is(sessionId)))
			.andExpect(jsonPath("$.canResumeSession", is(true)));

		createLostSession("bob");

		mockMvc.perform(get("/api/challenges/current/status").with(user("bob")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.canResumeSession", is(false)));
	}

	@Test
	void currentChallengeStatusDoesNotExposeBoardSnapshotOrInternalBoardData() throws Exception {
		createSession("alice");

		mockMvc.perform(get("/api/challenges/current/status").with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.board").doesNotExist())
			.andExpect(jsonPath("$.boardSnapshot").doesNotExist())
			.andExpect(jsonPath("$.cells").doesNotExist())
			.andExpect(jsonPath("$.internalSeed").doesNotExist())
			.andExpect(jsonPath("$.minePositions").doesNotExist());
	}

	@Test
	void sessionByIdOnlyReturnsSessionsOwnedByTheAuthenticatedUser() throws Exception {
		MvcResult createdSession = mockMvc.perform(post("/api/challenges/current/sessions").with(user("alice")))
			.andExpect(status().isCreated())
			.andReturn();

		String sessionId = JsonTestUtils.readString(createdSession, "$.sessionId");

		mockMvc.perform(get("/api/sessions/{sessionId}", sessionId).with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sessionId", is(sessionId)));

		mockMvc.perform(get("/api/sessions/{sessionId}", sessionId).with(user("bob")))
			.andExpect(status().isForbidden());
	}

	@Test
	void sessionByIdReturnsNotFoundForUnknownSession() throws Exception {
		mockMvc.perform(get("/api/sessions/{sessionId}", "ses_missing").with(user("alice")))
			.andExpect(status().isNotFound());
	}

	@Test
	void revealSafeCellUpdatesVisibleBoardAndClickCount() throws Exception {
		String sessionId = createSession("alice");
		ChallengeBoard board = realBoard(sessionId);
		CellCoordinate safeCell = firstSafeCellWithAdjacentMines(board);

		mockMvc.perform(revealRequest(sessionId, safeCell).with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.action.type", is("REVEAL")))
			.andExpect(jsonPath("$.action.result", is("SAFE_REVEAL")))
			.andExpect(jsonPath("$.session.performance.clickCount", is(1)))
			.andExpect(jsonPath("$.session.performance.errorCount", is(0)))
			.andExpect(jsonPath("$.session.lives.remainingLives", is(3)))
			.andExpect(jsonPath(cellStatePath(safeCell), is("REVEALED_SAFE")))
			.andExpect(jsonPath(cellAdjacentPath(safeCell), is(board.adjacentMineCount(safeCell.row(), safeCell.col()))));
	}

	@Test
	void revealZeroSafeCellAppliesCascadeAsOneClick() throws Exception {
		String sessionId = createSession("alice");
		ChallengeBoard board = realBoard(sessionId);
		CellCoordinate zeroCell = firstSafeCellWithAdjacentMineCount(board, 0);

		mockMvc.perform(revealRequest(sessionId, zeroCell).with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.action.result", is("SAFE_REVEAL_CASCADE")))
			.andExpect(jsonPath("$.session.performance.clickCount", is(1)))
			.andExpect(jsonPath("$.session.performance.errorCount", is(0)))
			.andExpect(jsonPath(cellStatePath(zeroCell), is("REVEALED_SAFE")));

		assertThat(revealedSafeCount(sessionId)).isGreaterThan(1);
	}

	@Test
	void revealMineConsumesOneLifeAndRevealsOnlyThatMine() throws Exception {
		String sessionId = createSession("alice");
		ChallengeBoard board = realBoard(sessionId);
		CellCoordinate mineCell = firstMineCell(board);

		mockMvc.perform(revealRequest(sessionId, mineCell).with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.action.result", is("MINE_HIT")))
			.andExpect(jsonPath("$.session.status", is("IN_PROGRESS")))
			.andExpect(jsonPath("$.session.lives.remainingLives", is(2)))
			.andExpect(jsonPath("$.session.performance.errorCount", is(1)))
			.andExpect(jsonPath("$.session.performance.clickCount", is(1)))
			.andExpect(jsonPath(cellStatePath(mineCell), is("REVEALED_MINE")));
	}

	@Test
	void revealMineOnLastLifeEndsSessionAsLost() throws Exception {
		String sessionId = createSession("alice");
		List<CellCoordinate> mines = mineCells(realBoard(sessionId));

		reveal(sessionId, mines.get(0), "alice")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.action.result", is("MINE_HIT")));
		reveal(sessionId, mines.get(1), "alice")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.action.result", is("MINE_HIT")));

		reveal(sessionId, mines.get(2), "alice")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.action.result", is("SESSION_LOST")))
			.andExpect(jsonPath("$.session.status", is("LOST")))
			.andExpect(jsonPath("$.session.lives.remainingLives", is(0)))
			.andExpect(jsonPath("$.session.performance.errorCount", is(3)))
			.andExpect(jsonPath("$.session.performance.clickCount", is(3)))
			.andExpect(jsonPath("$.session.endedAt").exists());
	}

	@Test
	void toggleFlagAddsAndRemovesFlagOnHiddenCell() throws Exception {
		String sessionId = createSession("alice");
		CellCoordinate cell = firstSafeCell(realBoard(sessionId));

		mockMvc.perform(toggleFlagRequest(sessionId, cell).with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.action.type", is("TOGGLE_FLAG")))
			.andExpect(jsonPath("$.action.result", is("FLAG_ADDED")))
			.andExpect(jsonPath("$.session.performance.clickCount", is(1)))
			.andExpect(jsonPath(cellStatePath(cell), is("FLAGGED")));

		mockMvc.perform(toggleFlagRequest(sessionId, cell).with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.action.result", is("FLAG_REMOVED")))
			.andExpect(jsonPath("$.session.performance.clickCount", is(2)))
			.andExpect(jsonPath(cellStatePath(cell), is("HIDDEN")));
	}

	@Test
	void revealActionRequiresSessionOwnership() throws Exception {
		String sessionId = createSession("alice");
		CellCoordinate cell = firstSafeCell(realBoard(sessionId));

		mockMvc.perform(revealRequest(sessionId, cell).with(user("bob")))
			.andExpect(status().isForbidden());
	}

	@Test
	void actionsRejectFinishedSessions() throws Exception {
		String sessionId = createLostSession("alice");
		CellCoordinate cell = firstSafeCell(realBoard(sessionId));

		mockMvc.perform(toggleFlagRequest(sessionId, cell).with(user("alice")))
			.andExpect(status().isConflict());
	}

	@Test
	void revealingAllSafeCellsWinsTheSession() throws Exception {
		String sessionId = createSession("alice");
		ChallengeBoard board = realBoard(sessionId);
		MvcResult lastRevealResponse = null;

		for (int row = 0; row < board.rows(); row++) {
			for (int col = 0; col < board.cols(); col++) {
				CellCoordinate cell = new CellCoordinate(row, col);
				GameSession session = session(sessionId);
				if (session.getStatus() == GameSessionStatus.WON) {
					break;
				}
				if (!board.hasMine(row, col) && cellState(session, cell) == CellState.HIDDEN) {
					lastRevealResponse = reveal(sessionId, cell, "alice")
						.andExpect(status().isOk())
						.andReturn();
				}
			}
		}

		assertThat(lastRevealResponse).isNotNull();
		assertThat(JsonTestUtils.readString(lastRevealResponse, "$.action.result")).isEqualTo("SESSION_WON");

		GameSession wonSession = session(sessionId);
		assertThat(wonSession.getStatus()).isEqualTo(GameSessionStatus.WON);
		assertThat(wonSession.getEndedAt()).isNotNull();
		assertThat(wonSession.getRemainingLives()).isEqualTo(3);
	}

	@Test
	void losingSessionRevealsAllMinesAndKeepsUnrevealedSafeCellsHidden() throws Exception {
		String sessionId = createLostSession("alice");
		ChallengeBoard board = realBoard(sessionId);
		GameSession lostSession = session(sessionId);

		assertThat(lostSession.getStatus()).isEqualTo(GameSessionStatus.LOST);
		assertThat(board.minePositions())
			.allSatisfy(mine -> assertThat(cellState(lostSession, new CellCoordinate(mine.row(), mine.col())))
				.isEqualTo(CellState.REVEALED_MINE));
		assertThat(hiddenSafeCount(lostSession, board)).isGreaterThan(0);
	}

	private String createLostSession(String userId) throws Exception {
		String sessionId = createSession(userId);
		List<CellCoordinate> mines = mineCells(realBoard(sessionId));
		reveal(sessionId, mines.get(0), userId).andExpect(status().isOk());
		reveal(sessionId, mines.get(1), userId).andExpect(status().isOk());
		reveal(sessionId, mines.get(2), userId).andExpect(status().isOk());
		return sessionId;
	}

	private String createWonSession(String userId) throws Exception {
		String sessionId = createSession(userId);
		ChallengeBoard board = realBoard(sessionId);

		for (int row = 0; row < board.rows(); row++) {
			for (int col = 0; col < board.cols(); col++) {
				GameSession session = session(sessionId);
				if (session.getStatus() == GameSessionStatus.WON) {
					return sessionId;
				}
				CellCoordinate cell = new CellCoordinate(row, col);
				if (!board.hasMine(row, col) && cellState(session, cell) == CellState.HIDDEN) {
					reveal(sessionId, cell, userId).andExpect(status().isOk());
				}
			}
		}

		assertThat(session(sessionId).getStatus()).isEqualTo(GameSessionStatus.WON);
		return sessionId;
	}

	private String createSession(String userId) throws Exception {
		MvcResult createdSession = mockMvc.perform(post("/api/challenges/current/sessions").with(user(userId)))
			.andExpect(status().isCreated())
			.andReturn();
		return JsonTestUtils.readString(createdSession, "$.sessionId");
	}

	private org.springframework.test.web.servlet.ResultActions reveal(
		String sessionId,
		CellCoordinate cell,
		String userId
	) throws Exception {
		return mockMvc.perform(revealRequest(sessionId, cell).with(user(userId)));
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder revealRequest(
		String sessionId,
		CellCoordinate cell
	) {
		return post("/api/sessions/{sessionId}/actions/reveal", sessionId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(actionBody(cell));
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder toggleFlagRequest(
		String sessionId,
		CellCoordinate cell
	) {
		return post("/api/sessions/{sessionId}/actions/toggle-flag", sessionId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(actionBody(cell));
	}

	private String actionBody(CellCoordinate cell) {
		return """
			{
			  "row": %d,
			  "col": %d
			}
			""".formatted(cell.row(), cell.col());
	}

	private ChallengeBoard realBoard(String sessionId) {
		return challengeBoardReconstructor.reconstruct(session(sessionId).getChallenge().getBoardDefinition());
	}

	private GameSession session(String sessionId) {
		return gameSessionRepository.findBySessionId(sessionId).orElseThrow();
	}

	private CellCoordinate firstMineCell(ChallengeBoard board) {
		return mineCells(board).getFirst();
	}

	private List<CellCoordinate> mineCells(ChallengeBoard board) {
		return board.minePositions().stream()
			.sorted((left, right) -> {
				int rowComparison = Integer.compare(left.row(), right.row());
				if (rowComparison != 0) {
					return rowComparison;
				}
				return Integer.compare(left.col(), right.col());
			})
			.map(mine -> new CellCoordinate(mine.row(), mine.col()))
			.toList();
	}

	private CellCoordinate firstSafeCell(ChallengeBoard board) {
		return firstSafeCellMatching(board, ignored -> true);
	}

	private CellCoordinate firstSafeCellWithAdjacentMines(ChallengeBoard board) {
		return firstSafeCellMatching(board, cell -> board.adjacentMineCount(cell.row(), cell.col()) > 0);
	}

	private CellCoordinate firstSafeCellWithAdjacentMineCount(ChallengeBoard board, int adjacentMineCount) {
		return firstSafeCellMatching(
			board,
			cell -> board.adjacentMineCount(cell.row(), cell.col()) == adjacentMineCount
		);
	}

	private CellCoordinate firstSafeCellMatching(
		ChallengeBoard board,
		java.util.function.Predicate<CellCoordinate> predicate
	) {
		for (int row = 0; row < board.rows(); row++) {
			for (int col = 0; col < board.cols(); col++) {
				CellCoordinate cell = new CellCoordinate(row, col);
				if (!board.hasMine(row, col) && predicate.test(cell)) {
					return cell;
				}
			}
		}
		throw new AssertionError("No matching safe cell found");
	}

	private long revealedSafeCount(String sessionId) {
		return session(sessionId).getBoardSnapshot().cells().stream()
			.flatMap(List::stream)
			.filter(cell -> cell.state() == CellState.REVEALED_SAFE)
			.count();
	}

	private long hiddenSafeCount(GameSession session, ChallengeBoard board) {
		long count = 0;
		for (int row = 0; row < board.rows(); row++) {
			for (int col = 0; col < board.cols(); col++) {
				if (!board.hasMine(row, col)
					&& cellState(session, new CellCoordinate(row, col)) == CellState.HIDDEN) {
					count++;
				}
			}
		}
		return count;
	}

	private CellState cellState(GameSession session, CellCoordinate cell) {
		return session.getBoardSnapshot().cells().get(cell.row()).get(cell.col()).state();
	}

	private String cellStatePath(CellCoordinate cell) {
		return "$.session.board.cells[%d][%d].state".formatted(cell.row(), cell.col());
	}

	private String cellAdjacentPath(CellCoordinate cell) {
		return "$.session.board.cells[%d][%d].adjacentMineCount".formatted(cell.row(), cell.col());
	}

	private record CellCoordinate(int row, int col) {
	}

}
