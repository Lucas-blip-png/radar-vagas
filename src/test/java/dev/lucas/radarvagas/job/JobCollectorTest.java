package dev.lucas.radarvagas.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.web.client.ResourceAccessException;

import dev.lucas.radarvagas.RadarProperties;
import dev.lucas.radarvagas.source.JobPosting;
import dev.lucas.radarvagas.source.JobSource;

@DataJpaTest
class JobCollectorTest {

	@Autowired
	JobRepository jobs;

	private static final RadarProperties PROPS = new RadarProperties(List.of(), null, "java", Duration.ofHours(6),
			30, 7, null);

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-02T12:00:00Z"), ZoneOffset.UTC);

	record FakeSource(String name, List<JobPosting> postings) implements JobSource {
		@Override
		public List<JobPosting> fetch() {
			return postings;
		}
	}

	static class BrokenSource implements JobSource {
		@Override
		public String name() {
			return "broken";
		}

		@Override
		public List<JobPosting> fetch() {
			throw new ResourceAccessException("connection refused");
		}
	}

	private static JobPosting post(String url, String title) {
		return new JobPosting("fake", url, title, "Acme", "Remoto", List.of(), "Spring Boot", null);
	}

	private JobCollector collector(JobSource... sources) {
		return new JobCollector(List.of(sources), jobs, new JobScorer(), PROPS, CLOCK);
	}

	@Test
	void savesMatchingJobsOnceAcrossRuns() {
		var source = new FakeSource("fake", List.of(
				post("https://jobs/1", "Desenvolvedor Java Júnior"),
				post("https://jobs/2", "Desenvolvedor Java Sênior"),
				post("https://jobs/3", "Designer UX")));

		var first = collector(source).collect();
		var second = collector(source).collect();

		assertThat(first.saved()).isEqualTo(1);
		assertThat(second.saved()).isZero();
		assertThat(jobs.findAll()).singleElement().satisfies(job -> {
			assertThat(job.getUrl()).isEqualTo("https://jobs/1");
			assertThat(job.getStatus()).isEqualTo(JobStatus.NOVA);
			assertThat(job.getFoundAt()).isEqualTo(Instant.parse("2026-10-02T12:00:00Z"));
		});
	}

	@Test
	void aFailingSourceDoesNotStopTheOthers() {
		var result = collector(new BrokenSource(),
				new FakeSource("fake", List.of(post("https://jobs/9", "Estágio Java")))).collect();

		assertThat(result.failedSources()).containsExactly("broken");
		assertThat(result.saved()).isEqualTo(1);
	}

	@Test
	void ignoresJobsBelowMinimumScore() {
		// back-end + remote but no Java/Spring/junior signal: 15 points, below the 30 threshold
		var result = collector(new FakeSource("fake", List.of(
				new JobPosting("fake", "https://jobs/5", "Desenvolvedor Back-end Go", "Acme", "Remoto", List.of(), "",
						null)))).collect();

		assertThat(result.read()).isEqualTo(1);
		assertThat(result.saved()).isZero();
	}

}
