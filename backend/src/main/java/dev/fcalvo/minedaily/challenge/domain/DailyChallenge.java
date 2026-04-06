package dev.fcalvo.minedaily.challenge.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "daily_challenge")
public class DailyChallenge {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "challenge_id", nullable = false, unique = true, length = 64)
	private String challengeId;

	@Column(name = "challenge_date", nullable = false, unique = true)
	private LocalDate challengeDate;

	@Column(name = "window_start_at", nullable = false)
	private OffsetDateTime windowStartAt;

	@Column(name = "window_end_at", nullable = false)
	private OffsetDateTime windowEndAt;

	@Column(name = "rollover_at", nullable = false)
	private OffsetDateTime rolloverAt;

	@Column(name = "timezone", nullable = false, length = 64)
	private String timezone;

	// Persisted with the challenge so the board can be reproduced later even if defaults change.
	@Embedded
	private ChallengeBoardDefinition boardDefinition;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	protected DailyChallenge() {
	}

	private DailyChallenge(
		String challengeId,
		LocalDate challengeDate,
		OffsetDateTime windowStartAt,
		OffsetDateTime windowEndAt,
		OffsetDateTime rolloverAt,
		String timezone,
		ChallengeBoardDefinition boardDefinition
	) {
		this.challengeId = challengeId;
		this.challengeDate = challengeDate;
		this.windowStartAt = windowStartAt;
		this.windowEndAt = windowEndAt;
		this.rolloverAt = rolloverAt;
		this.timezone = timezone;
		this.boardDefinition = boardDefinition;
	}

	public static DailyChallenge create(
		String challengeId,
		ChallengeWindow challengeWindow,
		ChallengeBoardDefinition boardDefinition
	) {
		return new DailyChallenge(
			challengeId,
			challengeWindow.challengeDate(),
			challengeWindow.windowStartAt(),
			challengeWindow.windowEndAt(),
			challengeWindow.rolloverAt(),
			challengeWindow.timezone(),
			boardDefinition
		);
	}

	@PrePersist
	void onCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = OffsetDateTime.now();
	}

	public String getChallengeId() {
		return challengeId;
	}

	public LocalDate getChallengeDate() {
		return challengeDate;
	}

	public OffsetDateTime getWindowStartAt() {
		return windowStartAt;
	}

	public OffsetDateTime getWindowEndAt() {
		return windowEndAt;
	}

	public OffsetDateTime getRolloverAt() {
		return rolloverAt;
	}

	public String getTimezone() {
		return timezone;
	}

	public ChallengeBoardDefinition getBoardDefinition() {
		return boardDefinition;
	}

}
