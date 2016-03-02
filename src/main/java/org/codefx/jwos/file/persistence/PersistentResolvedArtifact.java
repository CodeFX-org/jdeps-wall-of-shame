package org.codefx.jwos.file.persistence;

import org.codefx.jwos.artifact.ResolvedArtifact;

import java.util.List;

import static org.codefx.jwos.Util.transformToImmutableSet;
import static org.codefx.jwos.Util.transformToList;

public class PersistentResolvedArtifact {

	public PersistentArtifactCoordinates artifact;
	public List<PersistentArtifactCoordinates> dependees;

	public static PersistentResolvedArtifact from(ResolvedArtifact artifact) {
		PersistentResolvedArtifact persistent = new PersistentResolvedArtifact();
		persistent.artifact = PersistentArtifactCoordinates.from(artifact.coordinates());
		persistent.dependees = transformToList(artifact.dependees(), PersistentArtifactCoordinates::from);
		return persistent;
	}

	public ResolvedArtifact toArtifact() {
		return new ResolvedArtifact(
				artifact.toArtifact(),
				transformToImmutableSet(dependees, PersistentArtifactCoordinates::toArtifact)
		);
	}


}
