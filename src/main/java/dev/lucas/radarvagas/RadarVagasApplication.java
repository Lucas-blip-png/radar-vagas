package dev.lucas.radarvagas;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(RadarProperties.class)
public class RadarVagasApplication {

	public static void main(String[] args) {
		SpringApplication.run(RadarVagasApplication.class, args);
	}

	@Bean
	Clock clock() {
		return Clock.systemDefaultZone();
	}

}
