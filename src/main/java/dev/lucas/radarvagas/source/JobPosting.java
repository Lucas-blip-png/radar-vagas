package dev.lucas.radarvagas.source;

import java.time.Instant;
import java.util.List;

/** A job post as read from a source, before filtering and scoring. */
public record JobPosting(String source, String url, String title, String company, String location,
		List<String> tags, String body, Instant publishedAt) {
}
