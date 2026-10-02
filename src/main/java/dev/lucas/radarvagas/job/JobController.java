package dev.lucas.radarvagas.job;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dev.lucas.radarvagas.RadarProperties;

@RestController
@RequestMapping("/api")
public class JobController {

	private final JobRepository jobs;
	private final JobCollector collector;
	private final ApplicationMessage message;
	private final Clock clock;
	private final int followUpDays;

	public JobController(JobRepository jobs, JobCollector collector, ApplicationMessage message, Clock clock,
			RadarProperties props) {
		this.jobs = jobs;
		this.collector = collector;
		this.message = message;
		this.clock = clock;
		this.followUpDays = props.followUpDays();
	}

	public record StatusChange(@NotNull JobStatus status, @Size(max = 1000) String notes) {
	}

	/** Without a status filter, lists everything except discarded jobs. */
	@GetMapping("/jobs")
	public List<Job> list(@RequestParam(required = false) JobStatus status) {
		return status == null
				? jobs.findByStatusNotOrderByScoreDescFoundAtDesc(JobStatus.DESCARTADA)
				: jobs.findByStatusOrderByScoreDescFoundAtDesc(status);
	}

	@PatchMapping("/jobs/{id}")
	@Transactional
	public Job update(@PathVariable long id, @Valid @RequestBody StatusChange change) {
		Job job = find(id);
		job.changeStatus(change.status(), Instant.now(clock), LocalDate.now(clock).plusDays(followUpDays));
		if (change.notes() != null) {
			job.setNotes(change.notes());
		}
		return job;
	}

	@GetMapping(value = "/jobs/{id}/message", produces = MediaType.TEXT_PLAIN_VALUE)
	public String applicationMessage(@PathVariable long id) {
		return message.forJob(find(id));
	}

	@PostMapping("/collect")
	public JobCollector.Result collect() {
		return collector.collect();
	}

	private Job find(long id) {
		return jobs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

}
