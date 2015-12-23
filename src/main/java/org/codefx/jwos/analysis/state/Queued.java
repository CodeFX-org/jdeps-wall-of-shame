package org.codefx.jwos.analysis.state;

class Queued<R> implements ComputationState<R> {

	@Override
	public ComputationStateIdentifier state() {
		return ComputationStateIdentifier.QUEUED;
	}

	@Override
	public ComputationState<R> queued() {
		throw new IllegalStateException("A queued computation must not be queued again.");
	}

	@Override
	public ComputationState<R> started() {
		return new Started<>();
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
