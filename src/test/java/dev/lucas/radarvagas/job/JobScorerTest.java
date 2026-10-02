package dev.lucas.radarvagas.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import dev.lucas.radarvagas.source.JobPosting;

class JobScorerTest {

	private final JobScorer scorer = new JobScorer();

	private static JobPosting post(String title, String location, String body, String... tags) {
		return new JobPosting("test", "https://example.com/" + title.hashCode(), title, "Acme", location,
				List.of(tags), body, null);
	}

	@Test
	void remoteJuniorJavaJobScoresHigh() {
		var score = scorer.score(post("[Remoto] Desenvolvedor Java Júnior - Acme", "", "Spring Boot, PostgreSQL e Docker"));

		// remote + Java in title + Spring + junior + 2 stack terms (PostgreSQL, Docker)
		assertThat(score).hasValue(15 + 40 + 20 + 25 + 6);
	}

	@Test
	void campoGrandeOnSiteJobIsAcceptedAndPreferredOverRemote() {
		var local = scorer.score(post("Desenvolvedor Back-end Java", "Campo Grande - MS", ""));
		var remote = scorer.score(post("Desenvolvedor Back-end Java", "Remoto", ""));

		assertThat(local).isPresent();
		assertThat(local.getAsInt()).isGreaterThan(remote.getAsInt());
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			[Remoto] Desenvolvedor Java Sênior          | Remoto
			[Remoto] Tech Lead Java                     | Remoto
			[Remoto] Especialista Spring                | Remoto
			[São Paulo] Desenvolvedor Java Júnior       | São Paulo - SP
			[Remoto] Desenvolvedor Front-end React      | Remoto
			[Remoto - somente EUA/Canadá] Java Developer | Remoto
			Junior Java Developer                       | USA
			""")
	void rejectsSeniorOnSiteElsewhereAndNonBackendJobs(String title, String location) {
		assertThat(scorer.score(post(title, location, ""))).isEmpty();
	}

	@Test
	void acceptsInternationalRemoteJobsOpenToBrazil() {
		assertThat(scorer.score(post("Junior Java Developer", "Northern America, LATAM, Europe, APAC", ""))).isPresent();
		assertThat(scorer.score(post("Junior Java Developer", "Worldwide", ""))).isPresent();
	}

	@Test
	void plenoJobsArePenalizedButKept() {
		var junior = scorer.score(post("Desenvolvedor Java Júnior", "Remoto", ""));
		var pleno = scorer.score(post("Desenvolvedor Java Pleno", "Remoto", ""));

		assertThat(pleno).isPresent();
		assertThat(pleno.getAsInt()).isLessThan(junior.getAsInt());
	}

	@Test
	void matchesWholeWordsOnlyAndIgnoresAccents() {
		// "javascript" must not count as Java, "ms" inside a word must not count as Campo Grande/MS
		assertThat(scorer.score(post("Desenvolvedor JavaScript", "Remoto", "systems"))).isEmpty();
		assertThat(scorer.score(post("Estágio em Back-end", "Campo Grande", "Java"))).isPresent();
	}

	@Test
	void labelsCountLikeTheTitle() {
		assertThat(scorer.score(post("Desenvolvedor(a) de Software - Acme", "", "", "Java", "Remoto", "Júnior")))
			.hasValue(15 + 40 + 25);
	}

}
