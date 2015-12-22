package org.codefx.jwos.analysis.state;

import java.time.LocalDateTime;

class NotComputed<R> implements ComputationState<R> {

	NotComputed() {
		// nothing to do
	}

	@Override
	public ComputationStateIdentifier state() {
		return ComputationStateIdentifier.NOT_COMPUTED;
	}

	@Override
	public ComputationState<R> queued() {
		return new Queued<>();
	}

	@Override
	public ComputationState<R> started(LocalDateTime startTime) {
		return new Started<>(startTime);
	}

	@Override
	public ComputationState<R> failed(Exception exception) {
		return new Failed<>(exception);
	}

	@Override
	public ComputationState<R> succeeded(R result) {
		return new Succeeded<>(result);
	}
}
