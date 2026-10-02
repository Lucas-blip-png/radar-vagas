package dev.lucas.radarvagas.job;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import dev.lucas.radarvagas.RadarProperties;
import dev.lucas.radarvagas.source.JobPosting;
import dev.lucas.radarvagas.source.JobSource;

@Service
public class JobCollector {

	private static final Logger log = LoggerFactory.getLogger(JobCollector.class);

	private final List<JobSource> sources;
	private final JobRepository jobs;
	private final JobScorer scorer;
	private final int minScore;
	private final Clock clock;

	public JobCollector(List<JobSource> sources, JobRepository jobs, JobScorer scorer, RadarProperties props,
			Clock clock) {
		this.sources = sources;
		this.jobs = jobs;
		this.scorer = scorer;
		this.minScore = props.minScore();
		this.clock = clock;
	}

	public record Result(int read, int saved, List<String> failedSources) {
	}

	/** One unreachable source must not stop the others: it is logged and reported. */
	@Scheduled(fixedDelayString = "${radar.collect-interval}", initialDelayString = "${radar.initial-delay}")
	public Result collect() {
		int read = 0;
		int saved = 0;
		List<String> failed = new ArrayList<>();
		for (JobSource source : sources) {
			List<JobPosting> postings;
			try {
				postings = source.fetch();
			}
			catch (RestClientException e) {
				log.warn("source_failed source={} error={}", source.name(), e.getMessage());
				failed.add(source.name());
				continue;
			}
			for (JobPosting posting : postings) {
				read++;
				if (posting.url() == null || jobs.existsByUrl(posting.url())) {
					continue;
				}
				OptionalInt score = scorer.score(posting);
				if (score.isPresent() && score.getAsInt() >= minScore) {
					jobs.save(toJob(posting, score.getAsInt()));
					saved++;
				}
			}
		}
		log.info("collect_done read={} saved={} failed={}", read, saved, failed);
		return new Result(read, saved, failed);
	}

	private Job toJob(JobPosting p, int score) {
		return new Job(p.source(), cut(p.url(), 500), cut(p.title(), 300), cut(p.company(), 200),
				cut(p.location(), 200), cut(String.join(", ", p.tags()), 500), p.publishedAt(), score,
				Instant.now(clock));
	}

	private static String cut(String s, int max) {
		return s == null || s.length() <= max ? s : s.substring(0, max);
	}

}
