package dev.fcalvo.minedaily.session.infrastructure;

import dev.fcalvo.minedaily.session.domain.GameSession;
import dev.fcalvo.minedaily.session.domain.GameSessionStatus;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Minimal repository queries for session lifecycle: lookup by public id and by active session.
 */
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

	Optional<GameSession> findBySessionId(String sessionId);

	Optional<GameSession> findByUserIdAndChallengeChallengeIdAndStatus(
		String userId,
		String challengeId,
		GameSessionStatus status
	);

}
