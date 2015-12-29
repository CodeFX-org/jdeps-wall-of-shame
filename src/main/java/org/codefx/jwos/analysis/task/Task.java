package org.codefx.jwos.analysis.task;

public class Task<R> {

	private TaskState<R> state;

	public Task() {
		state = new NotComputed<>();
	}

	public void queued() {
		state = state.queued();
	}

	public void started() {
		state = state.started();
	}

	public void failed(Exception exception) {
		state = state.failed(exception);
	}

	public void succeeded(R result) {
		state = state.succeeded(result);
	}

	public Exception error() {
		if (!Failed.class.isInstance(state))
			throw new IllegalStateException("Only failed computations have an error.");
		return ((Failed) state).error();
	}

	public R result() {
		if (!Succeeded.class.isInstance(state))
			throw new IllegalStateException("Only succeeded computations have a result.");
		return ((Succeeded<R>) state).result();
	}

	public TaskStateIdentifier identifier() {
		return state.identifier();
	}

	@Override
	public String toString() {
		return state.toString();
	}
}
