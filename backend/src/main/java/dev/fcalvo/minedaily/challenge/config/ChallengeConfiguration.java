package dev.fcalvo.minedaily.challenge.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(ChallengeProperties.class)
public class ChallengeConfiguration {

	@Bean
	Clock businessClock() {
		return Clock.systemUTC();
	}

}
