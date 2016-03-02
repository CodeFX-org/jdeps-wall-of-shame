package org.codefx.jwos.file.persistence;// NOT_PUBLISHED

import org.codefx.jwos.artifact.ResolvedArtifact;

import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.codefx.jwos.Util.toImmutableSet;

public class PersistentResolvedArtifact {

	public PersistentArtifactCoordinates artifact;
	public Set<PersistentArtifactCoordinates> dependees;

	public static PersistentResolvedArtifact from(ResolvedArtifact artifact) {
		PersistentResolvedArtifact persistent = new PersistentResolvedArtifact();
		persistent.artifact = PersistentArtifactCoordinates.from(artifact.coordinates());
		persistent.dependees = artifact
				.dependees().stream()
				.map(PersistentArtifactCoordinates::from)
				.collect(toSet());
		return persistent;
	}

	public ResolvedArtifact toArtifact() {
		return new ResolvedArtifact(
				artifact.toArtifact(),
				dependees.stream()
						.map(PersistentArtifactCoordinates::toArtifact)
						.collect(toImmutableSet())
		);
	}


}
