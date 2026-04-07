package dev.fcalvo.minedaily.session.application;

import dev.fcalvo.minedaily.challenge.application.ChallengeQueryService;
import dev.fcalvo.minedaily.challenge.domain.ChallengeBoard;
import dev.fcalvo.minedaily.challenge.domain.ChallengeBoardReconstructor;
import dev.fcalvo.minedaily.challenge.domain.DailyChallenge;
import dev.fcalvo.minedaily.session.domain.BoardCellSnapshot;
import dev.fcalvo.minedaily.session.domain.BoardSnapshot;
import dev.fcalvo.minedaily.session.domain.CellState;
import dev.fcalvo.minedaily.session.domain.GameSession;
import dev.fcalvo.minedaily.session.domain.GameSessionStatus;
import dev.fcalvo.minedaily.session.infrastructure.GameSessionRepository;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
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
	private final ChallengeBoardReconstructor challengeBoardReconstructor;
	private final Clock businessClock;

	public GameSessionService(
		GameSessionRepository gameSessionRepository,
		ChallengeQueryService challengeQueryService,
		ChallengeBoardReconstructor challengeBoardReconstructor,
		Clock businessClock
	) {
		this.gameSessionRepository = gameSessionRepository;
		this.challengeQueryService = challengeQueryService;
		this.challengeBoardReconstructor = challengeBoardReconstructor;
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

	@Transactional
	public SessionActionResult reveal(String sessionId, String userId, int row, int col) {
		GameSession session = getOwnedSession(sessionId, userId);
		ensureSessionIsInProgress(session);
		ChallengeBoard challengeBoard = challengeBoardReconstructor.reconstruct(
			session.getChallenge().getBoardDefinition()
		);
		validateCoordinate(challengeBoard, row, col);

		BoardCellSnapshot targetCell = cellAt(session.getBoardSnapshot(), row, col);
		if (targetCell.state() != CellState.HIDDEN) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "Cell is not operable for reveal");
		}

		List<List<BoardCellSnapshot>> updatedCells = mutableCells(session.getBoardSnapshot());
		int remainingLives = session.getRemainingLives();
		int errorCount = session.getErrorCount();
		int clickCount = session.getClickCount() + 1;
		GameSessionStatus status = GameSessionStatus.IN_PROGRESS;
		OffsetDateTime endedAt = null;
		String actionResult;

		if (challengeBoard.hasMine(row, col)) {
			setCell(updatedCells, new BoardCellSnapshot(row, col, CellState.REVEALED_MINE, null));
			remainingLives--;
			errorCount++;
			if (remainingLives == 0) {
				revealAllMines(updatedCells, challengeBoard);
				status = GameSessionStatus.LOST;
				endedAt = OffsetDateTime.now(businessClock);
				actionResult = "SESSION_LOST";
			} else {
				actionResult = "MINE_HIT";
			}
		} else {
			RevealComputation revealComputation = revealSafeCell(updatedCells, challengeBoard, row, col);
			if (allSafeCellsRevealed(updatedCells, challengeBoard)) {
				status = GameSessionStatus.WON;
				endedAt = OffsetDateTime.now(businessClock);
				actionResult = "SESSION_WON";
			} else if (revealComputation.cascade()) {
				actionResult = "SAFE_REVEAL_CASCADE";
			} else {
				actionResult = "SAFE_REVEAL";
			}
		}

		BoardSnapshot updatedSnapshot = new BoardSnapshot(
			session.getBoardSnapshot().rows(),
			session.getBoardSnapshot().cols(),
			immutableCells(updatedCells)
		);
		session.applyGameplayUpdate(updatedSnapshot, remainingLives, errorCount, clickCount, status, endedAt);
		return new SessionActionResult("REVEAL", row, col, actionResult, gameSessionRepository.save(session));
	}

	@Transactional
	public SessionActionResult toggleFlag(String sessionId, String userId, int row, int col) {
		GameSession session = getOwnedSession(sessionId, userId);
		ensureSessionIsInProgress(session);
		ChallengeBoard challengeBoard = challengeBoardReconstructor.reconstruct(
			session.getChallenge().getBoardDefinition()
		);
		validateCoordinate(challengeBoard, row, col);

		BoardCellSnapshot targetCell = cellAt(session.getBoardSnapshot(), row, col);
		List<List<BoardCellSnapshot>> updatedCells = mutableCells(session.getBoardSnapshot());
		String actionResult;

		if (targetCell.state() == CellState.HIDDEN) {
			setCell(updatedCells, new BoardCellSnapshot(row, col, CellState.FLAGGED, null));
			actionResult = "FLAG_ADDED";
		} else if (targetCell.state() == CellState.FLAGGED) {
			setCell(updatedCells, new BoardCellSnapshot(row, col, CellState.HIDDEN, null));
			actionResult = "FLAG_REMOVED";
		} else {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "Cell is not operable for flag");
		}

		BoardSnapshot updatedSnapshot = new BoardSnapshot(
			session.getBoardSnapshot().rows(),
			session.getBoardSnapshot().cols(),
			immutableCells(updatedCells)
		);
		session.applyGameplayUpdate(
			updatedSnapshot,
			session.getRemainingLives(),
			session.getErrorCount(),
			session.getClickCount() + 1,
			GameSessionStatus.IN_PROGRESS,
			null
		);
		return new SessionActionResult("TOGGLE_FLAG", row, col, actionResult, gameSessionRepository.save(session));
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

	private void ensureSessionIsInProgress(GameSession session) {
		if (session.getStatus() != GameSessionStatus.IN_PROGRESS) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is already finished");
		}
	}

	private void validateCoordinate(ChallengeBoard challengeBoard, int row, int col) {
		if (!challengeBoard.isInBounds(row, col)) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "Cell coordinate is outside the board");
		}
	}

	private BoardCellSnapshot cellAt(BoardSnapshot boardSnapshot, int row, int col) {
		return boardSnapshot.cells().get(row).get(col);
	}

	private List<List<BoardCellSnapshot>> mutableCells(BoardSnapshot boardSnapshot) {
		List<List<BoardCellSnapshot>> rows = new ArrayList<>(boardSnapshot.rows());
		for (List<BoardCellSnapshot> row : boardSnapshot.cells()) {
			rows.add(new ArrayList<>(row));
		}
		return rows;
	}

	private List<List<BoardCellSnapshot>> immutableCells(List<List<BoardCellSnapshot>> cells) {
		return cells.stream()
			.map(List::copyOf)
			.toList();
	}

	private void setCell(List<List<BoardCellSnapshot>> cells, BoardCellSnapshot cell) {
		cells.get(cell.row()).set(cell.col(), cell);
	}

	private RevealComputation revealSafeCell(
		List<List<BoardCellSnapshot>> cells,
		ChallengeBoard challengeBoard,
		int row,
		int col
	) {
		int adjacentMineCount = challengeBoard.adjacentMineCount(row, col);
		if (adjacentMineCount > 0) {
			setCell(cells, new BoardCellSnapshot(row, col, CellState.REVEALED_SAFE, adjacentMineCount));
			return new RevealComputation(false);
		}

		ArrayDeque<CellCoordinate> pendingCells = new ArrayDeque<>();
		Set<CellCoordinate> visitedCells = new HashSet<>();
		pendingCells.add(new CellCoordinate(row, col));

		while (!pendingCells.isEmpty()) {
			CellCoordinate currentCell = pendingCells.removeFirst();
			if (!visitedCells.add(currentCell)) {
				continue;
			}
			if (!challengeBoard.isInBounds(currentCell.row(), currentCell.col())) {
				continue;
			}
			if (challengeBoard.hasMine(currentCell.row(), currentCell.col())) {
				continue;
			}

			BoardCellSnapshot currentSnapshot = cells.get(currentCell.row()).get(currentCell.col());
			if (currentSnapshot.state() != CellState.HIDDEN) {
				continue;
			}

			int currentAdjacentMineCount = challengeBoard.adjacentMineCount(currentCell.row(), currentCell.col());
			setCell(
				cells,
				new BoardCellSnapshot(
					currentCell.row(),
					currentCell.col(),
					CellState.REVEALED_SAFE,
					currentAdjacentMineCount
				)
			);

			if (currentAdjacentMineCount == 0) {
				addAdjacentCells(pendingCells, currentCell);
			}
		}
		return new RevealComputation(true);
	}

	private void addAdjacentCells(ArrayDeque<CellCoordinate> pendingCells, CellCoordinate cell) {
		for (int adjacentRow = cell.row() - 1; adjacentRow <= cell.row() + 1; adjacentRow++) {
			for (int adjacentCol = cell.col() - 1; adjacentCol <= cell.col() + 1; adjacentCol++) {
				if (adjacentRow == cell.row() && adjacentCol == cell.col()) {
					continue;
				}
				pendingCells.add(new CellCoordinate(adjacentRow, adjacentCol));
			}
		}
	}

	private boolean allSafeCellsRevealed(List<List<BoardCellSnapshot>> cells, ChallengeBoard challengeBoard) {
		for (int row = 0; row < challengeBoard.rows(); row++) {
			for (int col = 0; col < challengeBoard.cols(); col++) {
				if (!challengeBoard.hasMine(row, col)
					&& cells.get(row).get(col).state() != CellState.REVEALED_SAFE) {
					return false;
				}
			}
		}
		return true;
	}

	private void revealAllMines(List<List<BoardCellSnapshot>> cells, ChallengeBoard challengeBoard) {
		challengeBoard.minePositions()
			.forEach(minePosition -> setCell(
				cells,
				new BoardCellSnapshot(minePosition.row(), minePosition.col(), CellState.REVEALED_MINE, null)
			));
	}

	private record CellCoordinate(int row, int col) {
	}

	private record RevealComputation(boolean cascade) {
	}

}
