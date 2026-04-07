package dev.fcalvo.minedaily.session.config;

import dev.fcalvo.minedaily.session.api.dto.CurrentUserChallengeStatusView;
import dev.fcalvo.minedaily.session.api.dto.GameSessionView;
import dev.fcalvo.minedaily.session.api.dto.SessionActionResponse;
import dev.fcalvo.minedaily.session.application.CurrentUserChallengeStatus;
import dev.fcalvo.minedaily.session.application.SessionActionResult;
import dev.fcalvo.minedaily.session.domain.BoardCellSnapshot;
import dev.fcalvo.minedaily.session.domain.BoardSnapshot;
import dev.fcalvo.minedaily.session.domain.GameSession;
import java.util.List;

/**
 * Translates the persisted session model into the public API shape used by the frontend.
 */
public final class SessionApiMapper {

	private SessionApiMapper() {
	}

	public static GameSessionView toGameSessionView(GameSession session) {
		return new GameSessionView(
			session.getSessionId(),
			session.getChallenge().getChallengeId(),
			session.getChallenge().getChallengeDate(),
			session.getStatus(),
			session.getStartedAt(),
			session.getEndedAt(),
			new GameSessionView.LivesView(session.getMaxLives(), session.getRemainingLives()),
			new GameSessionView.PerformanceView(session.getErrorCount(), session.getClickCount()),
			toBoardSnapshotView(session.getBoardSnapshot())
		);
	}

	public static CurrentUserChallengeStatusView toCurrentUserChallengeStatusView(
		CurrentUserChallengeStatus status
	) {
		return new CurrentUserChallengeStatusView(
			status.challengeId(),
			status.challengeDate(),
			status.status(),
			status.hasPlayedCurrentChallenge(),
			status.hasActiveSession(),
			status.activeSessionId(),
			status.canStartSession(),
			status.canResumeSession(),
			status.leaderboardEligible(),
			status.remainingLives(),
			status.maxLives(),
			status.finishedOutcome(),
			status.sessionId()
		);
	}

	public static SessionActionResponse toSessionActionResponse(SessionActionResult result) {
		return new SessionActionResponse(
			new SessionActionResponse.ActionView(
				result.actionType(),
				result.row(),
				result.col(),
				result.result()
			),
			toGameSessionView(result.session())
		);
	}

	private static GameSessionView.BoardSnapshotView toBoardSnapshotView(BoardSnapshot boardSnapshot) {
		List<List<GameSessionView.CellView>> cells = boardSnapshot.cells().stream()
			.map(SessionApiMapper::toRow)
			.toList();
		return new GameSessionView.BoardSnapshotView(boardSnapshot.rows(), boardSnapshot.cols(), cells);
	}

	private static List<GameSessionView.CellView> toRow(List<BoardCellSnapshot> row) {
		return row.stream()
			.map(cell -> new GameSessionView.CellView(
				cell.row(),
				cell.col(),
				cell.state().name(),
				cell.adjacentMineCount()
			))
			.toList();
	}

}
