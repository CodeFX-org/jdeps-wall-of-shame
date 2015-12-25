package org.codefx.jwos.artifact;

import static java.util.Objects.requireNonNull;

/**
 * A project for which a computation failed.
 */
public final class FailedProject implements IdentifiesProject {

	private final ProjectCoordinates project;
	private final Exception error;

	public FailedProject(ProjectCoordinates project, Exception error) {
		this.project = requireNonNull(project, "The argument 'project' must not be null.");
		this.error = requireNonNull(error, "The argument 'error' must not be null.");
	}

	@Override
	public ProjectCoordinates coordinates() {
		return project;
	}

	public Exception error() {
		return error;
	}

	@Override
	public String toString() {
		return project + " (failed: " + error + ")";
	}

}
