package org.codefx.jwos.artifact;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * An artifact whose JAR has been downloaded.
 */
public final class DownloadedArtifact implements IdentifiesArtifact {

	private final ArtifactCoordinates artifact;
	private final Path path;

	public DownloadedArtifact(ArtifactCoordinates artifact, Path path) {
		this.artifact = requireNonNull(artifact, "The argument 'artifact' must not be null.");
		this.path = requireNonNull(path, "The argument 'path' must not be null.");
	}

	@Override
	public ArtifactCoordinates coordinates() {
		return artifact;
	}

	public Path path() {
		return path;
	}

	@Override
	public String toString() {
		return artifact + " (resolved)";
	}

}
