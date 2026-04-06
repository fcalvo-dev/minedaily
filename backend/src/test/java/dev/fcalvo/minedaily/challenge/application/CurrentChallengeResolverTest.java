package dev.fcalvo.minedaily.challenge.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.fcalvo.minedaily.challenge.config.ChallengeProperties;
import dev.fcalvo.minedaily.challenge.domain.ChallengeWindow;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class CurrentChallengeResolverTest {

	@Test
	void resolvesPreviousChallengeDateBeforeRollover() {
		ChallengeProperties properties = new ChallengeProperties();
		Clock fixedClock = Clock.fixed(Instant.parse("2026-04-07T23:30:00Z"), ZoneOffset.UTC);
		CurrentChallengeResolver resolver = new CurrentChallengeResolver(fixedClock, properties);

		ChallengeWindow window = resolver.resolveCurrentWindow();

		assertThat(window.challengeDate()).isEqualTo(LocalDate.of(2026, 4, 6));
		assertThat(window.windowStartAt().toString()).isEqualTo("2026-04-06T21:00-03:00");
		assertThat(window.rolloverAt().toString()).isEqualTo("2026-04-07T21:00-03:00");
		assertThat(window.windowEndAt().toString()).isEqualTo("2026-04-07T20:59:59.999-03:00");
	}

	@Test
	void resolvesCurrentChallengeDateAtRollover() {
		ChallengeProperties properties = new ChallengeProperties();
		Clock fixedClock = Clock.fixed(Instant.parse("2026-04-08T00:00:00Z"), ZoneOffset.UTC);
		CurrentChallengeResolver resolver = new CurrentChallengeResolver(fixedClock, properties);

		ChallengeWindow window = resolver.resolveCurrentWindow();

		assertThat(window.challengeDate()).isEqualTo(LocalDate.of(2026, 4, 7));
		assertThat(window.windowStartAt().toString()).isEqualTo("2026-04-07T21:00-03:00");
		assertThat(window.rolloverAt().toString()).isEqualTo("2026-04-08T21:00-03:00");
	}

}
