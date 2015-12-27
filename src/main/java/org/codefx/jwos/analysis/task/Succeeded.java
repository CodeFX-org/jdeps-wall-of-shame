package org.codefx.jwos.analysis.task;

import static java.util.Objects.requireNonNull;

class Succeeded<R> implements TaskState<R> {

	private final R result;

	Succeeded(R result) {
		this.result = requireNonNull(result, "The argument 'result' must not be null.");
	}

	public R result() {
		return result;
	}

	@Override
	public TaskStateIdentifier identifier() {
		return TaskStateIdentifier.SUCCEEDED;
	}

	@Override
	public TaskState<R> queued() {
		return new Queued<>();
	}

	@Override
	public TaskState<R> started() {
		throw new IllegalStateException("A succeeded computation must be queued before it can be started again.");
	}

	@Override
	public TaskState<R> failed(Exception exception) {
		throw new IllegalStateException("A succeeded computation must be queued and started before it can fail.");
	}

	@Override
	public TaskState<R> succeeded(R result) {
		throw new IllegalStateException(
				"A succeeded computation must be queued and started before it can succeed again.");
	}
}
