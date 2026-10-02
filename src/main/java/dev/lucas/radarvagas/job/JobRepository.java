package dev.lucas.radarvagas.job;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {

	boolean existsByUrl(String url);

	List<Job> findByStatusOrderByScoreDescFoundAtDesc(JobStatus status);

	List<Job> findByStatusNotOrderByScoreDescFoundAtDesc(JobStatus status);

}
