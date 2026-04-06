package dev.fcalvo.minedaily.challenge.infrastructure;

import dev.fcalvo.minedaily.challenge.domain.DailyChallenge;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyChallengeRepository extends JpaRepository<DailyChallenge, Long> {

	Optional<DailyChallenge> findByChallengeDate(LocalDate challengeDate);

}
