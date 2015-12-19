package org.codefx.jwos.analysis;

import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.connect.ThrowingFunction;

import java.util.Collection;

public class AnalysisFunctions {

	private final Analysis analysis;

	public AnalysisFunctions(Collection<DeeplyAnalyzedArtifact> formerlyAnalyzedArtifacts) {
		analysis = new Analysis(formerlyAnalyzedArtifacts);
	}

	public ThrowingFunction<ArtifactCoordinates, Collection<ArtifactCoordinates>> addToAnalyzeAsFunction() {
		return artifact -> {
			synchronized (analysis) {
				analysis.toAnalyse(artifact);
				return analysis.retrieveForAnalysis();
			}
		};
	}

	public ThrowingFunction<AnalyzedArtifact, Collection<DeeplyAnalyzedArtifact>> deepAnalyzeAsFunction() {
		return artifact -> {
			synchronized (analysis) {
				return analysis.analyzed(artifact);
			}
		};
	}

}
