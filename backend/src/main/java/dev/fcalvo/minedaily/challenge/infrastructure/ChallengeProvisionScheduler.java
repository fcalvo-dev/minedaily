package dev.fcalvo.minedaily.challenge.infrastructure;

import dev.fcalvo.minedaily.challenge.application.ChallengeProvisioningService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ChallengeProvisionScheduler {

	private final ChallengeProvisioningService challengeProvisioningService;

	public ChallengeProvisionScheduler(ChallengeProvisioningService challengeProvisioningService) {
		this.challengeProvisioningService = challengeProvisioningService;
	}

	@Scheduled(
		cron = "${minedaily.challenge.rollover-cron}",
		zone = "${minedaily.challenge.timezone}"
	)
	public void provisionNextChallengeWindow() {
		// The same service also backs lazy provisioning, so scheduled and on-demand creation stay consistent.
		challengeProvisioningService.provisionCurrentChallenge();
	}

}
