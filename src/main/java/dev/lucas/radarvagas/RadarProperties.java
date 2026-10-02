package dev.lucas.radarvagas;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("radar")
public record RadarProperties(
		List<String> githubRepos,
		String githubToken,
		String remotiveSearch,
		Duration collectInterval,
		int minScore,
		int followUpDays,
		Profile profile) {

	public record Profile(String name, String email, String github, String linkedin, String pitch,
			List<String> projects) {
	}

}
