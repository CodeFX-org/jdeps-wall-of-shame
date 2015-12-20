package org.codefx.jwos.analysis;

import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.connect.ThrowingFunction;

import java.util.Collection;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

public class AnalysisFunctions {

	private final Analysis analysis;

	public AnalysisFunctions(Collection<DeeplyAnalyzedArtifact> formerlyAnalyzedArtifacts) {
		analysis = new Analysis(formerlyAnalyzedArtifacts);
	}

	public ThrowingFunction<ArtifactCoordinates, Collection<ArtifactCoordinates>> startAnalysis() {
		return artifact -> {
			synchronized (analysis) {
				boolean hasToBeAnalyzed = analysis.startAnalysis(artifact);
				return hasToBeAnalyzed ? singleton(artifact) : emptySet();
			}
		};
	}

	public ThrowingFunction<ResolvedArtifact, Collection<ArtifactCoordinates>> resolvedDependencies() {
		return artifact -> {
			synchronized (analysis) {
				return analysis.resolved(artifact);
			}
		};
	}

	public ThrowingFunction<AnalyzedArtifact, Collection<DeeplyAnalyzedArtifact>> deepAnalysis() {
		return artifact -> {
			synchronized (analysis) {
				return analysis.analyzed(artifact);
			}
		};
	}

}
