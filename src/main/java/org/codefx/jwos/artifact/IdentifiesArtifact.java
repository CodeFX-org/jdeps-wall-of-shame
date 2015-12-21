package org.codefx.jwos.artifact;

import java.util.Comparator;

public interface IdentifiesArtifact {

	ArtifactCoordinates coordinates();

	static Comparator<IdentifiesArtifact> alphabeticalOrder() {
		return Comparator
				.<IdentifiesArtifact, String>comparing(id -> id.coordinates().groupId())
				.thenComparing(id -> id.coordinates().artifactId())
				.thenComparing(id -> id.coordinates().version());
	}

}
