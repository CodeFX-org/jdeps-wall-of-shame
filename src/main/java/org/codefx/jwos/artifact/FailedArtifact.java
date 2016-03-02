package org.codefx.jwos.artifact;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * An artifact for which a computation failed.
 */
public final class FailedArtifact implements IdentifiesArtifactTask<Exception> {

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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FailedArtifact that = (FailedArtifact) o;
		return Objects.equals(artifact, that.artifact) &&
				Objects.equals(error.getMessage(), that.error.getMessage());
	}

	@Override
	public int hashCode() {
		return Objects.hash(artifact, error.getMessage());
	}

	@Override
	public String toString() {
		return artifact + " (failed: " + error.getMessage() + ")";
	}

}
