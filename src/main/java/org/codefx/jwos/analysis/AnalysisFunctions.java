package org.codefx.jwos.analysis;

import org.codefx.jwos.connect.ThrowingFunction;
import org.codefx.jwos.maven.ArtifactCoordinates;

import java.util.Collection;

public class AnalysisFunctions {

	private final Analysis analysis;

	public AnalysisFunctions() {
		analysis = new Analysis();
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
