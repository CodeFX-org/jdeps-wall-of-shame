package org.codefx.jwos.artifact;

import java.util.Comparator;

public interface IdentifiesProject {

	ProjectCoordinates coordinates();

	static Comparator<IdentifiesProject> alphabeticalOrder() {
		return Comparator
				.<IdentifiesProject, String>comparing(id -> id.coordinates().groupId())
				.thenComparing(id -> id.coordinates().artifactId());
	}

}
