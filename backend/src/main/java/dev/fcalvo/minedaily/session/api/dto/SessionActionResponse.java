package dev.fcalvo.minedaily.session.api.dto;

public record SessionActionResponse(
	ActionView action,
	GameSessionView session
) {

	public record ActionView(String type, int row, int col, String result) {
	}

}
