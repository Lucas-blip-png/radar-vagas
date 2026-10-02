package dev.lucas.radarvagas.job;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import dev.lucas.radarvagas.RadarProperties;

/** Builds a ready-to-paste application message from the profile in application.yml. */
@Component
public class ApplicationMessage {

	private final RadarProperties.Profile profile;

	public ApplicationMessage(RadarProperties props) {
		this.profile = props.profile();
	}

	public String forJob(Job job) {
		String role = job.getTitle().replaceFirst("^\\s*\\[[^\\]]*\\]\\s*", "");
		String at = job.getCompany() == null ? "" : " na " + job.getCompany();
		String projects = profile.projects().stream().map(p -> "• " + p).collect(Collectors.joining("\n"));
		return """
				Olá! Vi a vaga "%s"%s e gostaria de me candidatar.

				%s

				Projetos:
				%s

				GitHub: %s
				LinkedIn: %s

				Obrigado pela atenção!
				%s — %s
				""".formatted(role, at, profile.pitch(), projects, profile.github(), profile.linkedin(),
				profile.name(), profile.email());
	}

}
