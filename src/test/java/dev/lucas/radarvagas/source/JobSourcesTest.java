package dev.lucas.radarvagas.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import dev.lucas.radarvagas.RadarProperties;

class JobSourcesTest {

	private static RadarProperties props(String token) {
		return new RadarProperties(List.of("backend-br/vagas"), token, "java", Duration.ofHours(6), 30, 7, null);
	}

	@Test
	void githubSourceParsesIssuesAndSkipsPullRequests() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://api.github.com/repos/backend-br/vagas/issues?state=open&per_page=100"))
			.andExpect(header("Authorization", "Bearer secret"))
			.andRespond(withSuccess(new ClassPathResource("fixtures/github-issues.json"), MediaType.APPLICATION_JSON));

		List<JobPosting> postings = new GitHubIssuesSource(builder, props("secret")).fetch();

		assertThat(postings).hasSize(2);
		JobPosting first = postings.get(0);
		assertThat(first.url()).isEqualTo("https://github.com/backend-br/vagas/issues/101");
		assertThat(first.location()).isEqualTo("Remoto");
		assertThat(first.company()).isEqualTo("Acme Tech");
		assertThat(first.tags()).contains("Java", "Júnior");
		assertThat(first.publishedAt()).isEqualTo(Instant.parse("2026-09-30T12:00:00Z"));
		server.verify();
	}

	@Test
	void remotiveSourceMapsJobs() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://remotive.com/api/remote-jobs?search=java"))
			.andRespond(withSuccess(new ClassPathResource("fixtures/remotive.json"), MediaType.APPLICATION_JSON));

		List<JobPosting> postings = new RemotiveSource(builder, props(null)).fetch();

		assertThat(postings).extracting(JobPosting::company).containsExactly("Globex", "Initech");
		assertThat(postings.get(0).location()).isEqualTo("Worldwide");
		assertThat(postings.get(0).publishedAt()).isEqualTo(Instant.parse("2026-09-21T12:55:11Z"));
		server.verify();
	}

}
