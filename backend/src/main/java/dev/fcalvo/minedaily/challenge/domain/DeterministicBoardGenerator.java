package dev.fcalvo.minedaily.challenge.domain;

import dev.fcalvo.minedaily.challenge.config.ChallengeProperties;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class DeterministicBoardGenerator implements BoardGenerator {

	private final ChallengeProperties challengeProperties;

	public DeterministicBoardGenerator(ChallengeProperties challengeProperties) {
		this.challengeProperties = challengeProperties;
	}

	@Override
	public GeneratedBoardDefinition generate(LocalDate challengeDate) {
		int rows = challengeProperties.getRows();
		int cols = challengeProperties.getCols();
		int mineCount = challengeProperties.getMineCount();

		if (mineCount >= rows * cols) {
			throw new IllegalStateException("mineCount must be lower than total board cells");
		}

		long internalSeed = deriveSeed(challengeDate);
		Set<MinePosition> minePositions = MineLayout.generateMinePositions(internalSeed, rows, cols, mineCount);

		return new GeneratedBoardDefinition(
			rows,
			cols,
			mineCount,
			internalSeed,
			challengeProperties.getGeneratorVersion(),
			MineLayout.fingerprint(minePositions)
		);
	}

	private long deriveSeed(LocalDate challengeDate) {
		// The seed is derived from stable inputs so the same challengeDate always rebuilds the same board.
		byte[] hash = MineLayout.sha256(
			challengeProperties.getSeedNamespace()
				+ "|"
				+ challengeProperties.getGeneratorVersion()
				+ "|"
				+ challengeDate
		);
		return ByteBuffer.wrap(hash, 0, Long.BYTES).getLong();
	}

}
