package org.codefx.jwos.analysis.state;

import java.time.LocalDateTime;

interface ComputationState<R> {

	// METHODS

	ComputationStateIdentifier state();

	ComputationState<R> queued();

	ComputationState<R> started(LocalDateTime startTime);

	ComputationState<R> failed(Exception exception);

	ComputationState<R> succeeded(R result);

}
