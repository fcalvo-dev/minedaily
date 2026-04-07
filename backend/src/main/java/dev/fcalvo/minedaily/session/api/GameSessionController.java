package dev.fcalvo.minedaily.session.api;

import dev.fcalvo.minedaily.session.api.dto.CurrentUserChallengeStatusView;
import dev.fcalvo.minedaily.session.api.dto.GameSessionView;
import dev.fcalvo.minedaily.session.api.dto.SessionActionRequest;
import dev.fcalvo.minedaily.session.api.dto.SessionActionResponse;
import dev.fcalvo.minedaily.session.application.CreateSessionResult;
import dev.fcalvo.minedaily.session.application.CurrentUserChallengeStatusService;
import dev.fcalvo.minedaily.session.application.GameSessionService;
import dev.fcalvo.minedaily.session.config.SessionApiMapper;
import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
/**
 * Exposes current-user session status, session lifecycle, and gameplay action endpoints.
 */
public class GameSessionController {

	private final GameSessionService gameSessionService;
	private final CurrentUserChallengeStatusService currentUserChallengeStatusService;

	public GameSessionController(
		GameSessionService gameSessionService,
		CurrentUserChallengeStatusService currentUserChallengeStatusService
	) {
		this.gameSessionService = gameSessionService;
		this.currentUserChallengeStatusService = currentUserChallengeStatusService;
	}

	@GetMapping("/challenges/current/status")
	public CurrentUserChallengeStatusView getCurrentUserChallengeStatus(Principal principal) {
		return SessionApiMapper.toCurrentUserChallengeStatusView(
			currentUserChallengeStatusService.getCurrentUserChallengeStatus(principal.getName())
		);
	}

	@PostMapping("/challenges/current/sessions")
	public ResponseEntity<GameSessionView> createOrReuseCurrentSession(Principal principal) {
		CreateSessionResult result = gameSessionService.createOrReuseCurrentSession(principal.getName());
		HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status)
			.body(SessionApiMapper.toGameSessionView(result.session()));
	}

	@GetMapping("/challenges/current/sessions/active")
	public ResponseEntity<GameSessionView> getActiveCurrentSession(Principal principal) {
		return gameSessionService.getActiveCurrentSession(principal.getName())
			.map(SessionApiMapper::toGameSessionView)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@GetMapping("/sessions/{sessionId}")
	public GameSessionView getSession(@PathVariable String sessionId, Principal principal) {
		return SessionApiMapper.toGameSessionView(
			gameSessionService.getOwnedSession(sessionId, principal.getName())
		);
	}

	@PostMapping("/sessions/{sessionId}/actions/reveal")
	public SessionActionResponse reveal(
		@PathVariable String sessionId,
		@RequestBody SessionActionRequest request,
		Principal principal
	) {
		return SessionApiMapper.toSessionActionResponse(
			gameSessionService.reveal(sessionId, principal.getName(), request.row(), request.col())
		);
	}

	@PostMapping("/sessions/{sessionId}/actions/toggle-flag")
	public SessionActionResponse toggleFlag(
		@PathVariable String sessionId,
		@RequestBody SessionActionRequest request,
		Principal principal
	) {
		return SessionApiMapper.toSessionActionResponse(
			gameSessionService.toggleFlag(sessionId, principal.getName(), request.row(), request.col())
		);
	}

}
