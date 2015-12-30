package org.codefx.jwos.jdeps;

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.DownloadedArtifact;
import org.codefx.jwos.jdeps.dependency.Violation;
import org.codefx.jwos.jdeps.exec.JdkInternalsExecutor;
import org.codefx.jwos.jdeps.parse.ViolationParser;
import org.codefx.jwos.jdeps.search.ComposedJDepsSearch;
import org.codehaus.plexus.util.cli.CommandLineException;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * Internal API for communication with JDeps.
 * <p>
 * Can analyze individual artifacts and return the parsed results.
 * <p>
 * This class is thread-safe and when used concurrently will start a separate JDeps run for each artifact.
 */
public class JDeps {

	private final Path jdeps;

	public JDeps(Path jdeps) {
		this.jdeps = requireNonNull(jdeps, "The argument 'jdeps' must not be null.");
	}

	public JDeps() {
		this(findJDeps());
	}

	private static Path findJDeps() {
		return new ComposedJDepsSearch()
				.search()
				.orElseThrow(() -> new IllegalStateException("Could not find JDeps executable."));
	}

	public AnalyzedArtifact analyze(DownloadedArtifact artifact) throws CommandLineException {
		ImmutableSet.Builder<Violation> violations = ImmutableSet.builder();
		ViolationParser violationParser = new ViolationParser(violations::add);
		new JdkInternalsExecutor(jdeps, artifact.path(), violationParser::parseLine).execute();
		violationParser.finish();
		return new AnalyzedArtifact(artifact.coordinates(), violations.build());
	}

}
