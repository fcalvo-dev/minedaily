package dev.fcalvo.minedaily.challenge.api;

import dev.fcalvo.minedaily.challenge.api.dto.CurrentChallengeView;
import dev.fcalvo.minedaily.challenge.api.dto.DailyLeaderboardView;
import dev.fcalvo.minedaily.challenge.application.ChallengeQueryService;
import dev.fcalvo.minedaily.challenge.application.CurrentChallengeLeaderboardService;
import dev.fcalvo.minedaily.challenge.config.ChallengeApiMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

	private final ChallengeQueryService challengeQueryService;
	private final CurrentChallengeLeaderboardService currentChallengeLeaderboardService;

	public ChallengeController(
		ChallengeQueryService challengeQueryService,
		CurrentChallengeLeaderboardService currentChallengeLeaderboardService
	) {
		this.challengeQueryService = challengeQueryService;
		this.currentChallengeLeaderboardService = currentChallengeLeaderboardService;
	}

	@GetMapping("/current")
	public CurrentChallengeView getCurrentChallenge() {
		return ChallengeApiMapper.toCurrentChallengeView(challengeQueryService.getCurrentChallenge());
	}

	@GetMapping("/current/leaderboard")
	public DailyLeaderboardView getCurrentChallengeLeaderboard() {
		return ChallengeApiMapper.toDailyLeaderboardView(
			currentChallengeLeaderboardService.getCurrentLeaderboard()
		);
	}

}
