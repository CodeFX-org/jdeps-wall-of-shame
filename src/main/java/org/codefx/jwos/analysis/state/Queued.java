package org.codefx.jwos.analysis.state;

import java.time.LocalDateTime;

class Queued<R> implements ComputationState<R> {

	Queued() {
		// nothing to do
	}

	@Override
	public ComputationStateIdentifier state() {
		return ComputationStateIdentifier.QUEUED;
	}

	@Override
	public ComputationState<R> queued() {
		throw new IllegalStateException("A queued computation must not be queued again.");
	}

	@Override
	public ComputationState<R> started(LocalDateTime startTime) {
		return new Started<>(startTime);
	}

	@Override
	public ComputationState<R> failed(Exception exception) {
		throw new IllegalStateException("A queued computation must be started before it can fail.");
	}

	@Override
	public ComputationState<R> succeeded(R result) {
		throw new IllegalStateException("A queued computation must be started before it can succeed.");
	}
}
