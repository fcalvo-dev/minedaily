package dev.fcalvo.minedaily.session.domain;

/**
 * One visible cell inside the persisted board snapshot.
 */
public record BoardCellSnapshot(
	int row,
	int col,
	CellState state,
	Integer adjacentMineCount
) {
}
