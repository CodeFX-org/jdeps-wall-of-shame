package org.codefx.jwos.computation;

import static java.util.Objects.requireNonNull;

public class TaskSink<T> implements Computation {

	private final ReceiveTask<T> receive;
	private final ComputeTask<T, Void> compute;
	private final SendError<T> sendError;

	public TaskSink(ReceiveTask<T> receive, ComputeTask<T, Void> compute, SendError<T> sendError) {
		this.receive = requireNonNull(receive, "The argument 'receive' must not be null.");
		this.compute =requireNonNull(compute,"The argument 'compute' must not be null.");
		this.sendError = requireNonNull(sendError, "The argument 'sendError' must not be null.");
	}

	@Override
	public void compute() throws InterruptedException {
		T task = receive.receive();
		try {
			compute.compute(task);
		} catch (Exception ex) {
			sendError.send(task, ex);
		}
	}

}
