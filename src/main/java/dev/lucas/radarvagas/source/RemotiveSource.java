package dev.lucas.radarvagas.source;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import dev.lucas.radarvagas.RadarProperties;

/**
 * Remote jobs from Remotive's public API. Their terms ask to link back to the original post
 * and not to republish the jobs elsewhere, so postings are only listed locally with the link.
 */
@Component
public class RemotiveSource implements JobSource {

	private final RestClient http;
	private final String search;

	public RemotiveSource(RestClient.Builder builder, RadarProperties props) {
		this.http = builder.baseUrl("https://remotive.com").build();
		this.search = props.remotiveSearch();
	}

	@Override
	public String name() {
		return "remotive";
	}

	@Override
	public List<JobPosting> fetch() {
		Response response = http.get()
			.uri("/api/remote-jobs?search={search}", search)
			.retrieve()
			.body(Response.class);
		if (response == null || response.jobs() == null) {
			return List.of();
		}
		return response.jobs().stream()
			.map(j -> new JobPosting(name(), j.url(), j.title(), j.company(), j.location(),
					j.tags() == null ? List.of() : j.tags(), j.description(),
					j.publishedAt() == null ? null : j.publishedAt().toInstant(ZoneOffset.UTC)))
			.toList();
	}

	record Response(List<Job> jobs) {
	}

	record Job(String url, String title, @JsonProperty("company_name") String company,
			@JsonProperty("candidate_required_location") String location, List<String> tags,
			String description, @JsonProperty("publication_date") LocalDateTime publishedAt) {
	}

}
