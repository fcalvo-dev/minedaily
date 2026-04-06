package dev.fcalvo.minedaily.challenge.application;

import dev.fcalvo.minedaily.challenge.config.ChallengeProperties;
import dev.fcalvo.minedaily.challenge.domain.ChallengeWindow;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Component;

@Component
public class CurrentChallengeResolver {

	private final Clock clock;
	private final ChallengeProperties challengeProperties;

	public CurrentChallengeResolver(Clock clock, ChallengeProperties challengeProperties) {
		this.clock = clock;
		this.challengeProperties = challengeProperties;
	}

	public ChallengeWindow resolveCurrentWindow() {
		ZoneId zoneId = timezoneId();
		LocalTime rolloverTime = LocalTime.of(challengeProperties.getRolloverHour(), 0);
		ZonedDateTime businessNow = ZonedDateTime.ofInstant(clock.instant(), zoneId);
		// Before the official rollover, the active challenge still belongs to the previous business day.
		LocalDate challengeDate = businessNow.toLocalTime().compareTo(rolloverTime) >= 0
			? businessNow.toLocalDate()
			: businessNow.toLocalDate().minusDays(1);
		return resolveWindow(challengeDate);
	}

	public ChallengeWindow resolveWindow(LocalDate challengeDate) {
		ZonedDateTime windowStart = challengeDate
			.atTime(challengeProperties.getRolloverHour(), 0)
			.atZone(timezoneId());
		ZonedDateTime rolloverAt = windowStart.plusDays(1);
		// The public API exposes an inclusive-looking end instant instead of the next window start.
		ZonedDateTime windowEnd = rolloverAt.minusNanos(1_000_000);

		return new ChallengeWindow(
			challengeDate,
			windowStart.toOffsetDateTime(),
			windowEnd.toOffsetDateTime(),
			rolloverAt.toOffsetDateTime(),
			challengeProperties.getTimezone()
		);
	}

	public ZoneId timezoneId() {
		return ZoneId.of(challengeProperties.getTimezone());
	}

}
