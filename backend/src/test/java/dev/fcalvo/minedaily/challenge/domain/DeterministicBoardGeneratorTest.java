package dev.fcalvo.minedaily.challenge.domain;

import static org.assertj.core.api.Assertions.assertThat;

import dev.fcalvo.minedaily.challenge.config.ChallengeProperties;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class DeterministicBoardGeneratorTest {

	@Test
	void generatesStableBoardDefinitionForSameChallengeDate() {
		ChallengeProperties properties = new ChallengeProperties();
		properties.setSeedNamespace("stable-seed");
		properties.setGeneratorVersion("v1");
		DeterministicBoardGenerator boardGenerator = new DeterministicBoardGenerator(properties);

		GeneratedBoardDefinition first = boardGenerator.generate(LocalDate.of(2026, 4, 6));
		GeneratedBoardDefinition second = boardGenerator.generate(LocalDate.of(2026, 4, 6));

		assertThat(second).isEqualTo(first);
		assertThat(first.rows()).isEqualTo(10);
		assertThat(first.cols()).isEqualTo(10);
		assertThat(first.mineCount()).isEqualTo(18);
	}

	@Test
	void generatesDifferentSeedForDifferentChallengeDate() {
		ChallengeProperties properties = new ChallengeProperties();
		DeterministicBoardGenerator boardGenerator = new DeterministicBoardGenerator(properties);

		GeneratedBoardDefinition first = boardGenerator.generate(LocalDate.of(2026, 4, 6));
		GeneratedBoardDefinition second = boardGenerator.generate(LocalDate.of(2026, 4, 7));

		assertThat(second.internalSeed()).isNotEqualTo(first.internalSeed());
		assertThat(second.boardFingerprint()).isNotEqualTo(first.boardFingerprint());
	}

}
