package org.codefx.jwos.computation;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class TaskSource<R> implements Computation {

	private final ComputeTask<Void, Optional<R>> compute;
	private final SendResult<R> sendResult;
	private final SendError<Void> sendError;

	public TaskSource(ComputeTask<Void, Optional<R>> compute, SendResult<R> sendResult, SendError<Void> sendError) {
		this.compute = requireNonNull(compute, "The argument 'compute' must not be null.");
		this.sendResult = requireNonNull(sendResult, "The argument 'sendResult' must not be null.");
		this.sendError = requireNonNull(sendError, "The argument 'sendError' must not be null.");
	}

	@Override
	public void compute() throws InterruptedException {
		Optional<R> result = computeNextResult();
		if (result.isPresent())
			sendResult.send(result.get());
		else
			throw new InterruptedException("This is a hack to report an exhausted source.");
	}

	private Optional<R> computeNextResult() throws InterruptedException {
		try {
			return compute.compute(null);
		} catch (Exception ex) {
			sendError.send(null, ex);
			return Optional.empty();
		}
	}

}
