package dev.lucas.radarvagas.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
class JobControllerTest {

	@Autowired
	MockMvcTester mvc;

	@Autowired
	JobRepository jobs;

	private long jobId;

	@BeforeEach
	void setUp() {
		jobs.deleteAll();
		jobId = jobs.save(new Job("github:backend-br/vagas", "https://github.com/backend-br/vagas/issues/1",
				"[Remoto] Desenvolvedor Java Júnior - Acme", "Acme", "Remoto", "Java", null, 80, Instant.now()))
			.getId();
	}

	@Test
	void markingAsAppliedSchedulesFollowUp() {
		var result = mvc.patch().uri("/api/jobs/{id}", jobId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"APLICADA\",\"notes\":\"Enviado pelo site\"}");

		assertThat(result).hasStatusOk();
		Job job = jobs.findById(jobId).orElseThrow();
		assertThat(job.getStatus()).isEqualTo(JobStatus.APLICADA);
		assertThat(job.getAppliedAt()).isNotNull();
		assertThat(job.getFollowUpOn()).isAfter(LocalDate.now());
		assertThat(job.getNotes()).isEqualTo("Enviado pelo site");
	}

	@Test
	void discardedJobsLeaveTheDefaultList() {
		mvc.patch().uri("/api/jobs/{id}", jobId).contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"DESCARTADA\"}").exchange();

		assertThat(mvc.get().uri("/api/jobs")).hasStatusOk().bodyJson().extractingPath("$.length()").isEqualTo(0);
		assertThat(mvc.get().uri("/api/jobs?status=DESCARTADA")).bodyJson().extractingPath("$.length()").isEqualTo(1);
	}

	@Test
	void messageMentionsTheRoleCompanyAndProfileLinks() {
		assertThat(mvc.get().uri("/api/jobs/{id}/message", jobId)).hasStatusOk().bodyText()
			.contains("Vi a vaga \"Desenvolvedor Java Júnior - Acme\" na Acme")
			.contains("https://github.com/Lucas-blip-png")
			.doesNotContain("[Remoto]");
	}

	@Test
	void rejectsUnknownStatusAndUnknownJob() {
		assertThat(mvc.patch().uri("/api/jobs/{id}", jobId).contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"CONTRATADO\"}")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.get().uri("/api/jobs/{id}/message", 999_999)).hasStatus(HttpStatus.NOT_FOUND);
	}

}
