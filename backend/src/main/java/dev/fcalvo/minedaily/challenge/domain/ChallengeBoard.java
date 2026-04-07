package dev.fcalvo.minedaily.challenge.domain;

import java.util.Set;

public record ChallengeBoard(
	int rows,
	int cols,
	Set<MinePosition> minePositions
) {

	public ChallengeBoard {
		minePositions = Set.copyOf(minePositions);
	}

	public boolean isInBounds(int row, int col) {
		return row >= 0 && row < rows && col >= 0 && col < cols;
	}

	public boolean hasMine(int row, int col) {
		return minePositions.contains(new MinePosition(row, col));
	}

	public int adjacentMineCount(int row, int col) {
		int count = 0;
		for (int adjacentRow = row - 1; adjacentRow <= row + 1; adjacentRow++) {
			for (int adjacentCol = col - 1; adjacentCol <= col + 1; adjacentCol++) {
				if (adjacentRow == row && adjacentCol == col) {
					continue;
				}
				if (isInBounds(adjacentRow, adjacentCol) && hasMine(adjacentRow, adjacentCol)) {
					count++;
				}
			}
		}
		return count;
	}

}
