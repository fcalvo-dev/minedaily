package dev.fcalvo.minedaily.session.api;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.fcalvo.minedaily.challenge.infrastructure.DailyChallengeRepository;
import dev.fcalvo.minedaily.session.infrastructure.GameSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class GameSessionApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private GameSessionRepository gameSessionRepository;

	@Autowired
	private DailyChallengeRepository dailyChallengeRepository;

	@BeforeEach
	void setUp() {
		gameSessionRepository.deleteAll();
		dailyChallengeRepository.deleteAll();
	}

	@Test
	void createCurrentSessionInitializesTheLifecycleState() throws Exception {
		mockMvc.perform(post("/api/challenges/current/sessions").with(user("alice")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.sessionId").exists())
			.andExpect(jsonPath("$.challengeId").exists())
			.andExpect(jsonPath("$.status", is("IN_PROGRESS")))
			.andExpect(jsonPath("$.lives.maxLives", is(3)))
			.andExpect(jsonPath("$.lives.remainingLives", is(3)))
			.andExpect(jsonPath("$.performance.errorCount", is(0)))
			.andExpect(jsonPath("$.performance.clickCount", is(0)))
			.andExpect(jsonPath("$.board.rows", is(10)))
			.andExpect(jsonPath("$.board.cols", is(10)))
			.andExpect(jsonPath("$.board.cells[0][0].state", is("HIDDEN")))
			.andExpect(jsonPath("$.board.cells[9][9].state", is("HIDDEN")));

		org.assertj.core.api.Assertions.assertThat(gameSessionRepository.count()).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(dailyChallengeRepository.count()).isEqualTo(1);
	}

	@Test
	void createCurrentSessionIsIdempotentWhileTheSessionIsStillInProgress() throws Exception {
		MvcResult firstResponse = mockMvc.perform(post("/api/challenges/current/sessions").with(user("alice")))
			.andExpect(status().isCreated())
			.andReturn();

		String sessionId = JsonTestUtils.readString(firstResponse, "$.sessionId");

		mockMvc.perform(post("/api/challenges/current/sessions").with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sessionId", is(sessionId)));

		org.assertj.core.api.Assertions.assertThat(gameSessionRepository.count()).isEqualTo(1);
	}

	@Test
	void activeCurrentSessionReturnsNoContentWhenTheUserHasNoSession() throws Exception {
		mockMvc.perform(get("/api/challenges/current/sessions/active").with(user("alice")))
			.andExpect(status().isNoContent());
	}

	@Test
	void activeCurrentSessionReturnsTheExistingSessionForTheCurrentChallenge() throws Exception {
		MvcResult createdSession = mockMvc.perform(post("/api/challenges/current/sessions").with(user("alice")))
			.andExpect(status().isCreated())
			.andReturn();

		String sessionId = JsonTestUtils.readString(createdSession, "$.sessionId");

		mockMvc.perform(get("/api/challenges/current/sessions/active").with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sessionId", is(sessionId)))
			.andExpect(jsonPath("$.status", is("IN_PROGRESS")));
	}

	@Test
	void sessionByIdOnlyReturnsSessionsOwnedByTheAuthenticatedUser() throws Exception {
		MvcResult createdSession = mockMvc.perform(post("/api/challenges/current/sessions").with(user("alice")))
			.andExpect(status().isCreated())
			.andReturn();

		String sessionId = JsonTestUtils.readString(createdSession, "$.sessionId");

		mockMvc.perform(get("/api/sessions/{sessionId}", sessionId).with(user("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sessionId", is(sessionId)));

		mockMvc.perform(get("/api/sessions/{sessionId}", sessionId).with(user("bob")))
			.andExpect(status().isForbidden());
	}

	@Test
	void sessionByIdReturnsNotFoundForUnknownSession() throws Exception {
		mockMvc.perform(get("/api/sessions/{sessionId}", "ses_missing").with(user("alice")))
			.andExpect(status().isNotFound());
	}

}
