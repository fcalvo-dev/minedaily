package dev.fcalvo.minedaily.challenge.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SplittableRandom;

final class MineLayout {

	private MineLayout() {
	}

	static Set<MinePosition> generateMinePositions(long internalSeed, int rows, int cols, int mineCount) {
		SplittableRandom random = new SplittableRandom(internalSeed);
		LinkedHashSet<Integer> mineIndexes = new LinkedHashSet<>();

		while (mineIndexes.size() < mineCount) {
			mineIndexes.add(random.nextInt(rows * cols));
		}

		LinkedHashSet<MinePosition> positions = new LinkedHashSet<>();
		mineIndexes.stream()
			.sorted()
			.map(index -> new MinePosition(index / cols, index % cols))
			.forEach(positions::add);
		return Set.copyOf(positions);
	}

	static String fingerprint(Set<MinePosition> minePositions) {
		String serializedLayout = minePositions.stream()
			.sorted((left, right) -> {
				int rowComparison = Integer.compare(left.row(), right.row());
				if (rowComparison != 0) {
					return rowComparison;
				}
				return Integer.compare(left.col(), right.col());
			})
			.map(position -> position.row() + ":" + position.col())
			.reduce((left, right) -> left + "|" + right)
			.orElse("");
		return HexFormat.of().formatHex(sha256(serializedLayout));
	}

	static byte[] sha256(String input) {
		try {
			return MessageDigest.getInstance("SHA-256")
				.digest(input.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 must be available", exception);
		}
	}

}
