package dev.fcalvo.minedaily.challenge.application;

import dev.fcalvo.minedaily.challenge.domain.BoardGenerator;
import dev.fcalvo.minedaily.challenge.domain.ChallengeBoardDefinition;
import dev.fcalvo.minedaily.challenge.domain.ChallengeWindow;
import dev.fcalvo.minedaily.challenge.domain.DailyChallenge;
import dev.fcalvo.minedaily.challenge.domain.GeneratedBoardDefinition;
import dev.fcalvo.minedaily.challenge.domain.ProvisionedChallenge;
import dev.fcalvo.minedaily.challenge.infrastructure.DailyChallengeRepository;
import java.time.LocalDate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChallengeProvisioningService {

	private final DailyChallengeRepository dailyChallengeRepository;
	private final CurrentChallengeResolver currentChallengeResolver;
	private final BoardGenerator boardGenerator;

	public ChallengeProvisioningService(
		DailyChallengeRepository dailyChallengeRepository,
		CurrentChallengeResolver currentChallengeResolver,
		BoardGenerator boardGenerator
	) {
		this.dailyChallengeRepository = dailyChallengeRepository;
		this.currentChallengeResolver = currentChallengeResolver;
		this.boardGenerator = boardGenerator;
	}

	@Transactional
	public ProvisionedChallenge provisionCurrentChallenge() {
		return provisionChallenge(currentChallengeResolver.resolveCurrentWindow().challengeDate());
	}

	@Transactional
	public ProvisionedChallenge provisionChallenge(LocalDate challengeDate) {
		// Fast path: if the challenge already exists, provisioning is a pure read.
		return dailyChallengeRepository.findByChallengeDate(challengeDate)
			.map(existingChallenge -> new ProvisionedChallenge(existingChallenge, false))
			.orElseGet(() -> createChallenge(challengeDate));
	}

	private ProvisionedChallenge createChallenge(LocalDate challengeDate) {
		ChallengeWindow challengeWindow = currentChallengeResolver.resolveWindow(challengeDate);
		GeneratedBoardDefinition generatedBoardDefinition = boardGenerator.generate(challengeDate);
		DailyChallenge newChallenge = DailyChallenge.create(
			challengeIdFor(challengeDate),
			challengeWindow,
			ChallengeBoardDefinition.from(generatedBoardDefinition)
		);

		try {
			return new ProvisionedChallenge(dailyChallengeRepository.saveAndFlush(newChallenge), true);
		} catch (DataIntegrityViolationException exception) {
			// If two requests race for the same challengeDate, the unique constraint decides the winner.
			return dailyChallengeRepository.findByChallengeDate(challengeDate)
				.map(existingChallenge -> new ProvisionedChallenge(existingChallenge, false))
				.orElseThrow(() -> exception);
		}
	}

	private String challengeIdFor(LocalDate challengeDate) {
		return "ch_" + challengeDate;
	}

}
