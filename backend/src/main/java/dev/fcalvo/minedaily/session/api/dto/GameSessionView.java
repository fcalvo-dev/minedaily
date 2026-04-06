package dev.fcalvo.minedaily.session.api.dto;

import dev.fcalvo.minedaily.session.domain.GameSessionStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Public session payload returned by lifecycle endpoints.
 */
public record GameSessionView(
	String sessionId,
	String challengeId,
	LocalDate challengeDate,
	GameSessionStatus status,
	OffsetDateTime startedAt,
	OffsetDateTime endedAt,
	LivesView lives,
	PerformanceView performance,
	BoardSnapshotView board
) {

	public record LivesView(int maxLives, int remainingLives) {
	}

	public record PerformanceView(int errorCount, int clickCount) {
	}

	public record BoardSnapshotView(int rows, int cols, List<List<CellView>> cells) {
	}

	public record CellView(int row, int col, String state, Integer adjacentMineCount) {
	}

}
