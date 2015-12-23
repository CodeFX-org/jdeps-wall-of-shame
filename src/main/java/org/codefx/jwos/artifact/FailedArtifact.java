package org.codefx.jwos.artifact;

import static java.util.Objects.requireNonNull;

/**
 * An artifact for which a computation failed.
 */
public final class FailedArtifact implements IdentifiesArtifactComputation<Exception> {

	private final ArtifactCoordinates artifact;
	private final Exception error;

	public FailedArtifact(ArtifactCoordinates artifact, Exception error) {
		this.artifact = requireNonNull(artifact, "The argument 'artifact' must not be null.");
		this.error = requireNonNull(error, "The argument 'error' must not be null.");
	}

	@Override
	public ArtifactCoordinates coordinates() {
		return artifact;
	}

	@Override
	public Exception result() {
		return error;
	}

	public Exception error() {
		return error;
	}

	@Override
	public String toString() {
		return artifact + " (failed: " + error + ")";
	}

}
