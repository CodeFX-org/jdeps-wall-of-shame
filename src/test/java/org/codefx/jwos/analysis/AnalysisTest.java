package org.codefx.jwos.analysis;

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.maven.ArtifactCoordinates;
import org.junit.Before;
import org.junit.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class AnalysisTest {

	private Analysis analysis;

	@Before
	public void setUp() {
		analysis = new Analysis();
	}

	@Test
	public void withSimpleArtifact() throws Exception {
		ArtifactCoordinates artifact = ArtifactCoordinates.from("g", "a", "v");
		analysis.toAnalyse(artifact);

		// added artifact can be retrieved for analysis
		ImmutableSet<ArtifactCoordinates> artifacts = analysis.retrieveForAnalysis();
		assertThat(artifacts).hasSize(1);
		assertThat(artifacts.contains(artifact));

		// since it has no dependency, it is immediately deeply analyzed
		AnalyzedArtifact analyzedArtifact = new AnalyzedArtifact(artifact, ImmutableSet.of(), ImmutableSet.of());
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
		analysis.toAnalyse(dependent);
		analysis.retrieveForAnalysis();

		// since dependent has a dependee, it is not yet deeply analyzed
		AnalyzedArtifact analyzedArtifact =
				new AnalyzedArtifact(dependent, ImmutableSet.of(), ImmutableSet.of(dependee));
		ImmutableSet<DeeplyAnalyzedArtifact> analyzedArtifacts = analysis.analyzed(analyzedArtifact);
		assertThat(analyzedArtifacts).hasSize(0);

		// dependee can be retrieved for analysis
		ImmutableSet<ArtifactCoordinates> artifacts = analysis.retrieveForAnalysis();
		assertThat(artifacts).containsExactly(dependee);

		// analyzing dependee must return both artifacts
		analyzedArtifact = new AnalyzedArtifact(dependee, ImmutableSet.of(), ImmutableSet.of());
		analyzedArtifacts = analysis.analyzed(analyzedArtifact);
		assertThat(analyzedArtifacts).hasSize(2);
	}

}