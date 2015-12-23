package org.codefx.jwos.analysis.state;

class Started<R> implements ComputationState<R> {

	@Override
	public ComputationStateIdentifier state() {
		return ComputationStateIdentifier.STARTED;
	}

	@Override
	public ComputationState<R> queued() {
		throw new IllegalStateException("A started computation must not be queued again.");
	}

	@Override
	public ComputationState<R> started() {
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
