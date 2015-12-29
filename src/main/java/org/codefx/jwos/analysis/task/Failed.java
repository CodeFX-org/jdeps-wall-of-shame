package org.codefx.jwos.analysis.task;

import static java.util.Objects.requireNonNull;

class Failed<R> implements TaskState<R> {

	private final Exception exception;

	Failed(Exception exception) {
		this.exception = requireNonNull(exception, "The argument 'exception' must not be null.");
	}

	public Exception error() {
		return exception;
	}

	@Override
	public TaskStateIdentifier identifier() {
		return TaskStateIdentifier.FAILED;
	}

	@Override
	public TaskState<R> queued() {
		return new Queued<>();
	}

	@Override
	public TaskState<R> started() {
		throw new IllegalStateException("A failed computation must be queued before it can be started again.");
	}

	@Override
	public TaskState<R> failed(Exception exception) {
		throw new IllegalStateException(
				"A failed computation must be queued and started before it can fail again.");
	}

	@Override
	public TaskState<R> succeeded(R result) {
		throw new IllegalStateException("A failed computation must be queued and started before it can succeed.");
	}

	@Override
	public String toString() {
		return "Failed: " + exception;
	}
}
