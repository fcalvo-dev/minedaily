package dev.fcalvo.minedaily.session.domain;

import dev.fcalvo.minedaily.challenge.domain.ChallengeBoardDefinition;
import dev.fcalvo.minedaily.challenge.domain.DailyChallenge;
import dev.fcalvo.minedaily.session.infrastructure.BoardSnapshotJsonConverter;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "game_session")
/**
 * Persists a user's visible session state for a single daily challenge.
 * In slice 2 the session is always created in progress with a fully hidden board.
 */
public class GameSession {

	private static final int INITIAL_LIVES = 3;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "session_id", nullable = false, unique = true, length = 64)
	private String sessionId;

	@Column(name = "user_id", nullable = false, length = 255)
	private String userId;

	@ManyToOne(optional = false)
	@JoinColumn(name = "daily_challenge_id", nullable = false)
	private DailyChallenge challenge;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 16)
	private GameSessionStatus status;

	@Column(name = "started_at", nullable = false)
	private OffsetDateTime startedAt;

	@Column(name = "ended_at")
	private OffsetDateTime endedAt;

	@Column(name = "max_lives", nullable = false)
	private int maxLives;

	@Column(name = "remaining_lives", nullable = false)
	private int remainingLives;

	@Column(name = "error_count", nullable = false)
	private int errorCount;

	@Column(name = "click_count", nullable = false)
	private int clickCount;

	@Convert(converter = BoardSnapshotJsonConverter.class)
	@Column(name = "board_snapshot", nullable = false, columnDefinition = "TEXT")
	private BoardSnapshot boardSnapshot;

	@Column(name = "active_session_key", unique = true, length = 512)
	private String activeSessionKey;

	protected GameSession() {
	}

	private GameSession(
		String sessionId,
		String userId,
		DailyChallenge challenge,
		OffsetDateTime startedAt,
		BoardSnapshot boardSnapshot
	) {
		this.sessionId = sessionId;
		this.userId = userId;
		this.challenge = challenge;
		this.status = GameSessionStatus.IN_PROGRESS;
		this.startedAt = startedAt;
		this.maxLives = INITIAL_LIVES;
		this.remainingLives = INITIAL_LIVES;
		this.errorCount = 0;
		this.clickCount = 0;
		this.boardSnapshot = boardSnapshot;
		syncActiveSessionKey();
	}

	public static GameSession create(
		String sessionId,
		String userId,
		DailyChallenge challenge,
		OffsetDateTime startedAt
	) {
		ChallengeBoardDefinition boardDefinition = challenge.getBoardDefinition();
		// The session starts from an entirely hidden board; slice 3 will mutate this snapshot.
		return new GameSession(
			sessionId,
			userId,
			challenge,
			startedAt,
			BoardSnapshot.hidden(boardDefinition.getRows(), boardDefinition.getCols())
		);
	}

	@PrePersist
	@PreUpdate
	void syncBeforeSave() {
		syncActiveSessionKey();
	}

	private void syncActiveSessionKey() {
		// This synthetic key lets the database enforce "at most one IN_PROGRESS session"
		// without modeling attempts yet.
		this.activeSessionKey = this.status == GameSessionStatus.IN_PROGRESS
			? this.userId + "::" + this.challenge.getChallengeId()
			: null;
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getUserId() {
		return userId;
	}

	public DailyChallenge getChallenge() {
		return challenge;
	}

	public GameSessionStatus getStatus() {
		return status;
	}

	public OffsetDateTime getStartedAt() {
		return startedAt;
	}

	public OffsetDateTime getEndedAt() {
		return endedAt;
	}

	public int getMaxLives() {
		return maxLives;
	}

	public int getRemainingLives() {
		return remainingLives;
	}

	public int getErrorCount() {
		return errorCount;
	}

	public int getClickCount() {
		return clickCount;
	}

	public BoardSnapshot getBoardSnapshot() {
		return boardSnapshot;
	}

}
