package org.codefx.jwos.artifact;

import java.util.Comparator;

public interface IdentifiesArtifact {

	ArtifactCoordinates artifact();

	static Comparator<IdentifiesArtifact> alphabeticalOrder() {
		return Comparator
				.<IdentifiesArtifact, String>comparing(id -> id.artifact().groupId())
				.thenComparing(id -> id.artifact().artifactId())
				.thenComparing(id -> id.artifact().version());
	}

}
