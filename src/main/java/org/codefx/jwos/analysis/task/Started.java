package org.codefx.jwos.analysis.task;

class Started<R> implements TaskState<R> {

	@Override
	public TaskStateIdentifier identifier() {
		return TaskStateIdentifier.STARTED;
	}

	@Override
	public TaskState<R> queued() {
		throw new IllegalStateException("A started computation must not be queued again.");
	}

	@Override
	public TaskState<R> started() {
		throw new IllegalStateException("A started computation must not be started again.");
	}

	@Override
	public TaskState<R> failed(Exception exception) {
		return new Failed<>(exception);
	}

	@Override
	public TaskState<R> succeeded(R result) {
		return new Succeeded<>(result);
	}
}
