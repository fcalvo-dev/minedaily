package dev.fcalvo.minedaily.challenge.config;

import dev.fcalvo.minedaily.challenge.api.dto.CurrentChallengeView;
import dev.fcalvo.minedaily.challenge.api.dto.DailyLeaderboardView;
import dev.fcalvo.minedaily.challenge.api.dto.ProvisionChallengeResponse;
import dev.fcalvo.minedaily.challenge.application.CurrentChallengeLeaderboard;
import dev.fcalvo.minedaily.challenge.domain.ChallengeBoardDefinition;
import dev.fcalvo.minedaily.challenge.domain.DailyChallenge;
import dev.fcalvo.minedaily.challenge.domain.ProvisionedChallenge;
public final class ChallengeApiMapper {

	private ChallengeApiMapper() {
	}

	public static CurrentChallengeView toCurrentChallengeView(DailyChallenge dailyChallenge) {
		ChallengeBoardDefinition boardDefinition = dailyChallenge.getBoardDefinition();
		return new CurrentChallengeView(
			dailyChallenge.getChallengeId(),
			dailyChallenge.getChallengeDate(),
			dailyChallenge.getTimezone(),
			dailyChallenge.getWindowStartAt(),
			dailyChallenge.getWindowEndAt(),
			dailyChallenge.getRolloverAt(),
			new CurrentChallengeView.BoardView(
				boardDefinition.getRows(),
				boardDefinition.getCols(),
				boardDefinition.getMineCount()
			)
		);
	}

	public static ProvisionChallengeResponse toProvisionChallengeResponse(ProvisionedChallenge provisionedChallenge) {
		DailyChallenge challenge = provisionedChallenge.challenge();
		return new ProvisionChallengeResponse(
			challenge.getChallengeId(),
			challenge.getChallengeDate(),
			provisionedChallenge.created()
		);
	}

	public static DailyLeaderboardView toDailyLeaderboardView(CurrentChallengeLeaderboard leaderboard) {
		return new DailyLeaderboardView(
			leaderboard.challengeId(),
			leaderboard.challengeDate(),
			leaderboard.entries().stream()
				.map(entry -> new DailyLeaderboardView.LeaderboardEntryView(
					entry.position(),
					entry.displayName(),
					entry.durationMs(),
					entry.errorCount(),
					entry.clickCount(),
					entry.remainingLives(),
					entry.endedAt()
				))
				.toList()
		);
	}

}
