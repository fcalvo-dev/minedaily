package dev.fcalvo.minedaily.challenge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ChallengeBoardDefinition {

	@Column(name = "rows", nullable = false)
	private int rows;

	@Column(name = "cols", nullable = false)
	private int cols;

	@Column(name = "mine_count", nullable = false)
	private int mineCount;

	@Column(name = "internal_seed", nullable = false)
	private long internalSeed;

	@Column(name = "generator_version", nullable = false, length = 32)
	private String generatorVersion;

	@Column(name = "board_fingerprint", nullable = false, length = 64)
	private String boardFingerprint;

	protected ChallengeBoardDefinition() {
	}

	private ChallengeBoardDefinition(
		int rows,
		int cols,
		int mineCount,
		long internalSeed,
		String generatorVersion,
		String boardFingerprint
	) {
		this.rows = rows;
		this.cols = cols;
		this.mineCount = mineCount;
		this.internalSeed = internalSeed;
		this.generatorVersion = generatorVersion;
		this.boardFingerprint = boardFingerprint;
	}

	public static ChallengeBoardDefinition from(GeneratedBoardDefinition generatedBoardDefinition) {
		// This remains an internal backend model; only the public board config is exposed by the API.
		return new ChallengeBoardDefinition(
			generatedBoardDefinition.rows(),
			generatedBoardDefinition.cols(),
			generatedBoardDefinition.mineCount(),
			generatedBoardDefinition.internalSeed(),
			generatedBoardDefinition.generatorVersion(),
			generatedBoardDefinition.boardFingerprint()
		);
	}

	public int getRows() {
		return rows;
	}

	public int getCols() {
		return cols;
	}

	public int getMineCount() {
		return mineCount;
	}

	public long getInternalSeed() {
		return internalSeed;
	}

	public String getGeneratorVersion() {
		return generatorVersion;
	}

	public String getBoardFingerprint() {
		return boardFingerprint;
	}

}
