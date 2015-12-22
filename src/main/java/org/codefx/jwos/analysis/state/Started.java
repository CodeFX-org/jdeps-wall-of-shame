package org.codefx.jwos.analysis.state;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

class Started<R> implements ComputationState<R> {

	final LocalDateTime startTime;

	Started(LocalDateTime startTime) {
		this.startTime = requireNonNull(startTime, "The argument 'started' must not be null.");
	}

	@Override
	public ComputationStateIdentifier state() {
		return ComputationStateIdentifier.STARTED;
	}

	@Override
	public ComputationState<R> queued() {
		throw new IllegalStateException("A started computation must not be queued again.");
	}

	@Override
	public ComputationState<R> started(LocalDateTime startTime) {
		throw new IllegalStateException("A started computation must not be started again.");
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
