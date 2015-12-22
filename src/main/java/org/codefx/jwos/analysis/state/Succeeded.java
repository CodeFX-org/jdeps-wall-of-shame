package org.codefx.jwos.analysis.state;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

class Succeeded<R> implements ComputationState<R> {

	final R result;

	Succeeded(R result) {
		this.result = requireNonNull(result, "The argument 'result' must not be null.");
	}

	@Override
	public ComputationStateIdentifier state() {
		return ComputationStateIdentifier.SUCCEEDED;
	}

	@Override
	public ComputationState<R> queued() {
		return new Queued<>();
	}

	@Override
	public ComputationState<R> started(LocalDateTime startTime) {
		throw new IllegalStateException("A succeeded computation must be queued before it can be started again.");
	}

	@Override
	public ComputationState<R> failed(Exception exception) {
		throw new IllegalStateException("A succeeded computation must be queued and started before it can fail.");
	}

	@Override
	public ComputationState<R> succeeded(R result) {
		throw new IllegalStateException(
				"A succeeded computation must be queued and started before it can succeed again.");
	}
}
