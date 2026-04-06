package dev.fcalvo.minedaily.challenge.application;

import dev.fcalvo.minedaily.challenge.domain.DailyChallenge;
import org.springframework.stereotype.Service;

@Service
public class ChallengeQueryService {

	private final ChallengeProvisioningService challengeProvisioningService;

	public ChallengeQueryService(ChallengeProvisioningService challengeProvisioningService) {
		this.challengeProvisioningService = challengeProvisioningService;
	}

	public DailyChallenge getCurrentChallenge() {
		return challengeProvisioningService.provisionCurrentChallenge().challenge();
	}

}
