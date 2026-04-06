package dev.fcalvo.minedaily.session.domain;

/**
 * Session states supported by the API contract.
 * Slice 2 only creates IN_PROGRESS sessions; WON and LOST are reserved for slice 3.
 */
public enum GameSessionStatus {

	IN_PROGRESS,
	WON,
	LOST

}
