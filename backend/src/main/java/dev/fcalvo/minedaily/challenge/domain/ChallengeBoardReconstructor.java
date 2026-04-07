package dev.fcalvo.minedaily.challenge.domain;

import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class ChallengeBoardReconstructor {

	public ChallengeBoard reconstruct(ChallengeBoardDefinition boardDefinition) {
		Set<MinePosition> minePositions = MineLayout.generateMinePositions(
			boardDefinition.getInternalSeed(),
			boardDefinition.getRows(),
			boardDefinition.getCols(),
			boardDefinition.getMineCount()
		);
		String fingerprint = MineLayout.fingerprint(minePositions);
		if (!fingerprint.equals(boardDefinition.getBoardFingerprint())) {
			throw new IllegalStateException("Persisted challenge board fingerprint does not match regenerated board");
		}
		return new ChallengeBoard(boardDefinition.getRows(), boardDefinition.getCols(), minePositions);
	}

}
