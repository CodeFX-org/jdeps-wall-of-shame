package org.codefx.jwos.analysis;

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.artifact.InternalDependencies;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

public class AnalysisTest {

	private Analysis analysis;

	@Before
	public void setUp() {
		analysis = new Analysis(emptySet());
	}

	@Test
	public void withSimpleArtifact() throws Exception {
		ArtifactCoordinates artifact = ArtifactCoordinates.from("g", "a", "v");

		// added artifact must be analyzed
		boolean mustBeAnalyzed = analysis.startAnalysis(artifact);
		assertThat(mustBeAnalyzed).isTrue();

		// since it has no dependency, it is immediately deeply analyzed
		AnalyzedArtifact analyzedArtifact = new AnalyzedArtifact(artifact, ImmutableSet.of());
		ImmutableSet<DeeplyAnalyzedArtifact> analyzedArtifacts = analysis.analyzed(analyzedArtifact);
		assertThat(analyzedArtifacts).hasSize(1);

		DeeplyAnalyzedArtifact deeplyAnalyzedArtifact = analyzedArtifacts.iterator().next();
		assertThat(deeplyAnalyzedArtifact.artifact()).isSameAs(artifact);
		assertThat(deeplyAnalyzedArtifact.marker()).isSameAs(InternalDependencies.NONE);
		assertThat(deeplyAnalyzedArtifact.violations()).isEmpty();
		assertThat(deeplyAnalyzedArtifact.dependees()).isEmpty();
	}

	@Test
	public void withDependency() throws Exception {
		ArtifactCoordinates dependent = ArtifactCoordinates.from("g", "dependant ->", "v");
		ArtifactCoordinates dependee = ArtifactCoordinates.from("g", "-> dependee", "v");
		analysis.startAnalysis(dependent);

		// tell 'analysis' about the dependee
		analysis.resolved(new ResolvedArtifact(dependent, Paths.get(""), ImmutableSet.of(dependee)));

		// since dependent has a dependee, it is not yet deeply analyzed
		AnalyzedArtifact analyzedArtifact = new AnalyzedArtifact(dependent, ImmutableSet.of());
		ImmutableSet<DeeplyAnalyzedArtifact> analyzedArtifacts = analysis.analyzed(analyzedArtifact);
		assertThat(analyzedArtifacts).hasSize(0);

		// analyzing dependee must return both artifacts
		analyzedArtifact = new AnalyzedArtifact(dependee, ImmutableSet.of());
		analyzedArtifacts = analysis.analyzed(analyzedArtifact);
		assertThat(analyzedArtifacts).hasSize(2);
	}

}