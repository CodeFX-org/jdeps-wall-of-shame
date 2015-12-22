package org.codefx.jwos.analysis.state;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

public class Computation<R> {

	private ComputationState<R> state;

	public Computation() {
		state = new NotComputed<>();
	}

	public void queued() {
		state = state.queued();
	}

	public void  started(LocalDateTime startTime) {
		state = state.started(startTime);
	}

	public void  failed(Exception exception) {
		state = state.failed(exception);
	}

	public void  succeeded(R result) {
		state = state.succeeded(result);
	}

	public LocalDateTime startTime() {
		if (!Started.class.isInstance(state))
			throw new IllegalStateException("Only started computations have a start time.");
		return ((Started) state).startTime;
	}

	public Exception error() {
		if (!Failed.class.isInstance(state))
			throw new IllegalStateException("Only failed computations have an error.");
		return ((Failed) state).exception;
	}

	public R result() {
		if (!Succeeded.class.isInstance(state))
			throw new IllegalStateException("Only succeeded computations have a result.");
		return ((Succeeded<R>) state).result;
	}

}
