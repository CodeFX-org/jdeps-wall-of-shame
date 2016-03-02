package org.codefx.jwos.artifact;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * A project for which a computation failed.
 */
public final class FailedProject implements IdentifiesProjectTask<Exception> {

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
		FailedProject that = (FailedProject) o;
		return Objects.equals(project, that.project) &&
				Objects.equals(error.getMessage(), that.error.getMessage());
	}

	@Override
	public int hashCode() {
		return Objects.hash(project, error.getMessage());
	}

	@Override
	public String toString() {
		return project + " (failed: " + error + ")";
	}

}
