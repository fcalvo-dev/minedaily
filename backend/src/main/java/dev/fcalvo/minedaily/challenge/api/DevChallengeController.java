package dev.fcalvo.minedaily.challenge.api;

import dev.fcalvo.minedaily.challenge.api.dto.ProvisionChallengeRequest;
import dev.fcalvo.minedaily.challenge.api.dto.ProvisionChallengeResponse;
import dev.fcalvo.minedaily.challenge.application.ChallengeProvisioningService;
import dev.fcalvo.minedaily.challenge.config.ChallengeApiMapper;
import dev.fcalvo.minedaily.challenge.domain.ProvisionedChallenge;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/dev/challenges")
@ConditionalOnProperty(prefix = "minedaily.dev.endpoints", name = "enabled", havingValue = "true")
public class DevChallengeController {

	private final ChallengeProvisioningService challengeProvisioningService;

	public DevChallengeController(ChallengeProvisioningService challengeProvisioningService) {
		this.challengeProvisioningService = challengeProvisioningService;
	}

	@PostMapping("/provision")
	public ProvisionChallengeResponse provisionChallenge(
		@RequestBody(required = false) ProvisionChallengeRequest request
	) {
		// Omitting challengeDate is convenient for local testing against the currently active business window.
		ProvisionedChallenge provisionedChallenge = request == null || request.challengeDate() == null
			? challengeProvisioningService.provisionCurrentChallenge()
			: challengeProvisioningService.provisionChallenge(request.challengeDate());
		return ChallengeApiMapper.toProvisionChallengeResponse(provisionedChallenge);
	}

}
