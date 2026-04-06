package dev.fcalvo.minedaily.session.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Full visible board snapshot returned by the backend.
 * For the MVP we persist the whole visible state instead of per-move deltas.
 */
public record BoardSnapshot(
	int rows,
	int cols,
	List<List<BoardCellSnapshot>> cells
) {

	public BoardSnapshot {
		cells = List.copyOf(cells);
	}

	public static BoardSnapshot hidden(int rows, int cols) {
		List<List<BoardCellSnapshot>> hiddenCells = new ArrayList<>(rows);
		for (int row = 0; row < rows; row++) {
			List<BoardCellSnapshot> rowCells = new ArrayList<>(cols);
			for (int col = 0; col < cols; col++) {
				rowCells.add(new BoardCellSnapshot(row, col, CellState.HIDDEN, null));
			}
			hiddenCells.add(List.copyOf(rowCells));
		}
		return new BoardSnapshot(rows, cols, hiddenCells);
	}

}
