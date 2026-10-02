package dev.lucas.radarvagas.job;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "job")
public class Job {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String source;

	private String url;

	private String title;

	private String company;

	private String location;

	private String tags;

	private Instant publishedAt;

	private int score;

	@Enumerated(EnumType.STRING)
	private JobStatus status = JobStatus.NOVA;

	private Instant appliedAt;

	private LocalDate followUpOn;

	private String notes;

	private Instant foundAt;

	protected Job() {
	}

	public Job(String source, String url, String title, String company, String location, String tags,
			Instant publishedAt, int score, Instant foundAt) {
		this.source = source;
		this.url = url;
		this.title = title;
		this.company = company;
		this.location = location;
		this.tags = tags;
		this.publishedAt = publishedAt;
		this.score = score;
		this.foundAt = foundAt;
	}

	/** Moving to APLICADA stamps the date and schedules a follow-up reminder. */
	public void changeStatus(JobStatus newStatus, Instant now, LocalDate followUpOn) {
		if (newStatus == JobStatus.APLICADA && this.status != JobStatus.APLICADA) {
			this.appliedAt = now;
			this.followUpOn = followUpOn;
		}
		if (newStatus != JobStatus.APLICADA) {
			this.followUpOn = null;
		}
		this.status = newStatus;
	}

	public void setNotes(String notes) { this.notes = notes; }

	public Long getId() { return id; }
	public String getSource() { return source; }
	public String getUrl() { return url; }
	public String getTitle() { return title; }
	public String getCompany() { return company; }
	public String getLocation() { return location; }
	public String getTags() { return tags; }
	public Instant getPublishedAt() { return publishedAt; }
	public int getScore() { return score; }
	public JobStatus getStatus() { return status; }
	public Instant getAppliedAt() { return appliedAt; }
	public LocalDate getFollowUpOn() { return followUpOn; }
	public String getNotes() { return notes; }
	public Instant getFoundAt() { return foundAt; }

}
