package dev.fcalvo.minedaily.challenge.api;

import static org.hamcrest.Matchers.is;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ChallengeApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DailyChallengeRepository dailyChallengeRepository;

	@Autowired
	private GameSessionRepository gameSessionRepository;

	@BeforeEach
	void setUp() {
		gameSessionRepository.deleteAll();
		dailyChallengeRepository.deleteAll();
	}

	@Test
	void currentChallengeEndpointLazilyProvisionsAndReturnsPublicShape() throws Exception {
		mockMvc.perform(get("/api/challenges/current"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.challengeId").exists())
			.andExpect(jsonPath("$.challengeDate").exists())
			.andExpect(jsonPath("$.timezone", is("America/Argentina/Cordoba")))
			.andExpect(jsonPath("$.board.rows", is(10)))
			.andExpect(jsonPath("$.board.cols", is(10)))
			.andExpect(jsonPath("$.board.mineCount", is(18)))
			.andExpect(jsonPath("$.internalSeed").doesNotExist());

		org.assertj.core.api.Assertions.assertThat(dailyChallengeRepository.count()).isEqualTo(1);
	}

	@Test
	void manualProvisionEndpointIsIdempotentForTheSameChallengeDate() throws Exception {
		String requestBody = """
			{
			  "challengeDate": "2026-04-06"
			}
			""";

		mockMvc.perform(post("/internal/dev/challenges/provision")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.challengeId", is("ch_2026-04-06")))
			.andExpect(jsonPath("$.created", is(true)));

		mockMvc.perform(post("/internal/dev/challenges/provision")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.challengeId", is("ch_2026-04-06")))
			.andExpect(jsonPath("$.created", is(false)));

		org.assertj.core.api.Assertions.assertThat(dailyChallengeRepository.count()).isEqualTo(1);
	}

}
