package dev.fcalvo.minedaily.challenge.domain;

public record GeneratedBoardDefinition(
	int rows,
	int cols,
	int mineCount,
	long internalSeed,
	String generatorVersion,
	String boardFingerprint
) {
}
