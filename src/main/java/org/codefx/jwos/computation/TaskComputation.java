package org.codefx.jwos.computation;

import static java.util.Objects.requireNonNull;

public class TaskComputation<T, R> implements Computation {

	private final ReceiveTask<T> receive;
	private final ComputeTask<T, R> compute;
	private final SendResult<R> sendResult;
	private final SendError<T> sendError;

	public TaskComputation(
			ReceiveTask<T> receive,
			ComputeTask<T, R> compute,
			SendResult<R> sendResult,
			SendError<T> sendError) {
		this.receive = requireNonNull(receive, "The argument 'receive' must not be null.");
		this.compute = requireNonNull(compute, "The argument 'compute' must not be null.");
		this.sendResult = requireNonNull(sendResult, "The argument 'sendResult' must not be null.");
		this.sendError = requireNonNull(sendError, "The argument 'sendError' must not be null.");
	}

	@Override
	public void compute() throws InterruptedException {
		T task = receive.receive();
		try {
			R result = compute.compute(task);
			sendResult.send(result);
		} catch (Exception ex) {
			sendError.send(task, ex);
		}
	}

}
