package org.codefx.jwos.analysis.state;

interface ComputationState<R> {

	// METHODS

	ComputationStateIdentifier state();

	ComputationState<R> queued();

	ComputationState<R> started();

	ComputationState<R> failed(Exception exception);

	ComputationState<R> succeeded(R result);

}
