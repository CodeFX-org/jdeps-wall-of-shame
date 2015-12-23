package org.codefx.jwos.analysis.state;

import static java.util.Objects.requireNonNull;

class Failed<R> implements ComputationState<R> {

	private final Exception exception;

	Failed(Exception exception) {
		this.exception = requireNonNull(exception, "The argument 'exception' must not be null.");
	}

	public Exception error() {
		return exception;
	}

	@Override
	public ComputationStateIdentifier state() {
		return ComputationStateIdentifier.FAILED;
	}

	@Override
	public ComputationState<R> queued() {
		return new Queued<>();
	}

	@Override
	public ComputationState<R> started() {
		throw new IllegalStateException("A failed computation must be queued before it can be started again.");
	}

	@Override
	public ComputationState<R> failed(Exception exception) {
		throw new IllegalStateException(
				"A failed computation must be queued and started before it can fail again.");
	}

	@Override
	public ComputationState<R> succeeded(R result) {
		throw new IllegalStateException("A failed computation must be queued and started before it can succeed.");
	}
}
