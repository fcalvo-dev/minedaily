package dev.fcalvo.minedaily.challenge.domain;

import dev.fcalvo.minedaily.challenge.config.ChallengeProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SplittableRandom;

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
		List<MinePosition> minePositions = generateMinePositions(internalSeed, rows, cols, mineCount);

		return new GeneratedBoardDefinition(
			rows,
			cols,
			mineCount,
			internalSeed,
			challengeProperties.getGeneratorVersion(),
			fingerprint(minePositions)
		);
	}

	private long deriveSeed(LocalDate challengeDate) {
		// The seed is derived from stable inputs so the same challengeDate always rebuilds the same board.
		byte[] hash = sha256(
			challengeProperties.getSeedNamespace()
				+ "|"
				+ challengeProperties.getGeneratorVersion()
				+ "|"
				+ challengeDate
		);
		return ByteBuffer.wrap(hash, 0, Long.BYTES).getLong();
	}

	private List<MinePosition> generateMinePositions(long internalSeed, int rows, int cols, int mineCount) {
		SplittableRandom random = new SplittableRandom(internalSeed);
		LinkedHashSet<Integer> mineIndexes = new LinkedHashSet<>();

		// LinkedHashSet avoids duplicates while preserving insertion order until we normalize with sort().
		while (mineIndexes.size() < mineCount) {
			mineIndexes.add(random.nextInt(rows * cols));
		}

		return mineIndexes.stream()
			.sorted()
			.map(index -> new MinePosition(index / cols, index % cols))
			.toList();
	}

	private String fingerprint(List<MinePosition> minePositions) {
		// Fingerprint lets us verify internally that a regenerated board matches the persisted one.
		String serializedLayout = minePositions.stream()
			.map(position -> position.row() + ":" + position.col())
			.reduce((left, right) -> left + "|" + right)
			.orElse("");
		return HexFormat.of().formatHex(sha256(serializedLayout));
	}

	private byte[] sha256(String input) {
		try {
			return MessageDigest.getInstance("SHA-256")
				.digest(input.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 must be available", exception);
		}
	}

}
