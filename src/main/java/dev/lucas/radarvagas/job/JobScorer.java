package dev.lucas.radarvagas.job;

import java.text.Normalizer;
import java.util.OptionalInt;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import dev.lucas.radarvagas.source.JobPosting;

/**
 * Decides whether a post fits the profile (junior Java/back-end, remote or Campo Grande/MS)
 * and how well. Empty means rejected.
 */
@Component
public class JobScorer {

	private static final Pattern SENIOR = words(
			"senior|sr|especialista|staff|lead|lider|principal|arquiteto|architect|head|gerente|manager|coordenador");
	private static final Pattern BACKEND = words("java|spring|spring boot|kotlin|back-end|backend|back end");
	private static final Pattern JAVA = words("java|spring|spring boot");
	private static final Pattern SPRING = words("spring|spring boot");
	private static final Pattern JUNIOR = words(
			"junior|jr|estagio|estagiario|estagiaria|trainee|intern|internship|entry level|aprendiz");
	private static final Pattern PLENO = words("pleno|mid|mid-level");
	private static final Pattern REMOTE = words(
			"remoto|remota|remote|home office|anywhere|worldwide|latam|brazil|brasil|americas|south america");
	private static final Pattern LOCAL = words("campo grande|mato grosso do sul|ms");
	// Remote, but only for people living there ("somente EUA/Canadá", Remotive's "USA")
	private static final Pattern RESTRICTED = words(
			"eua|usa|us|united states|canada|europe|europa|uk|united kingdom|germany|india");
	private static final Pattern BRAZIL_FRIENDLY = words(
			"brazil|brasil|latam|americas|south america|worldwide|anywhere");
	private static final Pattern STACK = words(
			"postgres|postgresql|sql|docker|rabbitmq|kafka|junit|testes automatizados|testcontainers|rest|microservices");

	public OptionalInt score(JobPosting posting) {
		String title = normalize(posting.title());
		String tags = normalize(String.join(" ", posting.tags()));
		String headline = title + " " + tags;
		String location = normalize(posting.location()) + " " + headline;
		String all = headline + " " + normalize(posting.body());

		// The stack must show up in the title or labels: descriptions often list the company's
		// whole stack, which would let a designer or sales job through.
		if (SENIOR.matcher(headline).find() || !BACKEND.matcher(headline).find()) {
			return OptionalInt.empty();
		}
		boolean local = LOCAL.matcher(location).find();
		if (!local && !REMOTE.matcher(location).find()) {
			return OptionalInt.empty();
		}
		if (RESTRICTED.matcher(location).find() && !BRAZIL_FRIENDLY.matcher(location).find()) {
			return OptionalInt.empty();
		}

		int score = local ? 20 : 15;
		score += JAVA.matcher(headline).find() ? 40 : JAVA.matcher(all).find() ? 20 : 0;
		score += SPRING.matcher(all).find() ? 20 : 0;
		score += JUNIOR.matcher(headline).find() ? 25 : 0;
		score -= PLENO.matcher(headline).find() ? 10 : 0;
		score += Math.min(15, 3 * (int) STACK.matcher(all).results().count());
		return OptionalInt.of(score);
	}

	private static Pattern words(String alternatives) {
		return Pattern.compile("(?<![\\p{L}\\d])(" + alternatives + ")(?![\\p{L}\\d])");
	}

	static String normalize(String text) {
		if (text == null) {
			return "";
		}
		return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
	}

}
