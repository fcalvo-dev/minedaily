package dev.fcalvo.minedaily.session.domain;

/**
 * Visible cell states that may appear in a persisted board snapshot.
 */
public enum CellState {

	HIDDEN,
	FLAGGED,
	REVEALED_SAFE,
	REVEALED_MINE

}
