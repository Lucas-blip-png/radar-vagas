package dev.lucas.radarvagas.source;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import dev.lucas.radarvagas.RadarProperties;

/**
 * Brazilian community job boards (e.g. backend-br/vagas) publish each opening as a GitHub
 * issue, with labels for stack, level and work model. Uses the public GitHub REST API.
 */
@Component
public class GitHubIssuesSource implements JobSource {

	private static final Pattern BRACKET = Pattern.compile("^\\s*\\[([^\\]]+)\\]\\s*(.*)$");

	private final RestClient http;
	private final List<String> repos;

	public GitHubIssuesSource(RestClient.Builder builder, RadarProperties props) {
		RestClient.Builder b = builder.baseUrl("https://api.github.com")
			.defaultHeader("Accept", "application/vnd.github+json");
		if (props.githubToken() != null && !props.githubToken().isBlank()) {
			b.defaultHeader("Authorization", "Bearer " + props.githubToken());
		}
		this.http = b.build();
		this.repos = props.githubRepos();
	}

	@Override
	public String name() {
		return "github";
	}

	@Override
	public List<JobPosting> fetch() {
		List<JobPosting> postings = new ArrayList<>();
		for (String repo : repos) {
			String[] ownerAndName = repo.split("/", 2);
			Issue[] issues = http.get()
				.uri("/repos/{owner}/{name}/issues?state=open&per_page=100", ownerAndName[0], ownerAndName[1])
				.retrieve()
				.body(Issue[].class);
			for (Issue issue : issues == null ? new Issue[0] : issues) {
				if (issue.pullRequest() == null) {
					postings.add(toPosting(repo, issue));
				}
			}
		}
		return postings;
	}

	private JobPosting toPosting(String repo, Issue issue) {
		String location = null;
		String rest = issue.title();
		Matcher m = BRACKET.matcher(issue.title());
		if (m.matches()) {
			location = m.group(1).strip();
			rest = m.group(2);
		}
		int dash = rest.lastIndexOf(" - ");
		String company = dash >= 0 ? rest.substring(dash + 3).strip() : null;
		List<String> tags = issue.labels() == null ? List.of() : issue.labels().stream().map(Label::name).toList();
		return new JobPosting(name() + ":" + repo, issue.url(), issue.title(), company, location, tags,
				issue.body(), issue.createdAt());
	}

	record Issue(String title, @JsonProperty("html_url") String url, String body,
			@JsonProperty("created_at") Instant createdAt, List<Label> labels,
			@JsonProperty("pull_request") Object pullRequest) {
	}

	record Label(String name) {
	}

}
