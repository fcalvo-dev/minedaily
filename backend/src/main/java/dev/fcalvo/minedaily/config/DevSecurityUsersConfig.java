package dev.fcalvo.minedaily.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@ConditionalOnProperty(prefix = "minedaily.dev.user", name = "enabled", havingValue = "true")
public class DevSecurityUsersConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService(
		PasswordEncoder passwordEncoder,
		@Value("${minedaily.dev.user.username}") String username,
		@Value("${minedaily.dev.user.password}") String password
	) {
		// This temporary in-memory user exists only to make manual endpoint testing possible
		// while the project still relies on principal.getName() as the session owner.
		return new InMemoryUserDetailsManager(
			User.withUsername(username)
				.password(passwordEncoder.encode(password))
				.roles("USER")
				.build()
		);
	}

}
