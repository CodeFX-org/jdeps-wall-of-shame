package org.codefx.jwos.analysis.task;

class Queued<R> implements TaskState<R> {

	@Override
	public TaskStateIdentifier identifier() {
		return TaskStateIdentifier.QUEUED;
	}

	@Override
	public TaskState<R> queued() {
		throw new IllegalStateException("A queued computation must not be queued again.");
	}

	@Override
	public TaskState<R> started() {
		return new Started<>();
	}

	@Override
	public TaskState<R> failed(Exception exception) {
		throw new IllegalStateException("A queued computation must be started before it can fail.");
	}

	@Override
	public TaskState<R> succeeded(R result) {
		throw new IllegalStateException("A queued computation must be started before it can succeed.");
	}

	@Override
	public String toString() {
		return "Queued...";
	}
}
