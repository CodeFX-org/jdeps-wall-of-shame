package org.codefx.jwos.analysis.task;

class NotComputed<R> implements TaskState<R> {

	@Override
	public TaskStateIdentifier identifier() {
		return TaskStateIdentifier.NOT_COMPUTED;
	}

	@Override
	public TaskState<R> queued() {
		return new Queued<>();
	}

	@Override
	public TaskState<R> started() {
		return new Started<>();
	}

	@Override
	public TaskState<R> failed(Exception exception) {
		return new Failed<>(exception);
	}

	@Override
	public TaskState<R> succeeded(R result) {
		return new Succeeded<>(result);
	}

	@Override
	public String toString() {
		return "Not computed";
	}
}
