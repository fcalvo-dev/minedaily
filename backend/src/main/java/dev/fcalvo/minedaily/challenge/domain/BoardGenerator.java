package dev.fcalvo.minedaily.challenge.domain;

import java.time.LocalDate;

public interface BoardGenerator {

	GeneratedBoardDefinition generate(LocalDate challengeDate);

}
