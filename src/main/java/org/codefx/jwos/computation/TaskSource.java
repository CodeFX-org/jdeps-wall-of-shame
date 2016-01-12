package org.codefx.jwos.computation;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Computes a task along these steps:
 * <ol>
 *     <li>try to {@link ComputeTask perform} a new task (e.g. read a new project form a web service)
 *     <li>send the {@link SendResult result} or {@link SendError error} (e.g. the project coordinates or exception)
 * </ol>
 *
 * @param <R> the type of the task's result if successful
 */
public class TaskSource<R> implements Computation {

	private final String name;
	private final ComputeTask<Void, Optional<R>> compute;
	private final SendResult<R> sendResult;
	private final SendError<Void> sendError;

	public TaskSource(String name, ComputeTask<Void, Optional<R>> compute, SendResult<R> sendResult, SendError<Void> sendError) {
		this.name = requireNonNull(name, "The argument 'name' must not be null.");
		this.compute = requireNonNull(compute, "The argument 'compute' must not be null.");
		this.sendResult = requireNonNull(sendResult, "The argument 'sendResult' must not be null.");
		this.sendError = requireNonNull(sendError, "The argument 'sendError' must not be null.");
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public void compute() throws InterruptedException {
		Optional<R> result = computeNextResult();
		if (result.isPresent())
			sendResult.send(result.get());
		else
			// the source is exhausted 
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
