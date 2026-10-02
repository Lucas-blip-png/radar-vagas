package dev.lucas.radarvagas.source;

import java.util.List;

public interface JobSource {

	String name();

	List<JobPosting> fetch();

}
