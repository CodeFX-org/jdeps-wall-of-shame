package org.codefx.jwos.analysis.state;

class NotComputed<R> implements ComputationState<R> {

	@Override
	public ComputationStateIdentifier state() {
		return ComputationStateIdentifier.NOT_COMPUTED;
	}

	@Override
	public ComputationState<R> queued() {
		return new Queued<>();
	}

	@Override
	public ComputationState<R> started() {
		return new Started<>();
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
