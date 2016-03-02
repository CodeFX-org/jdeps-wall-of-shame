package org.codefx.jwos.analysis.channel;

import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

abstract class AbstractTaskChannelDecorator<T, R, E> implements TaskChannel<T, R, E> {
	
	private final TaskChannel<T, R, E> decoratedChannel;

	protected AbstractTaskChannelDecorator(TaskChannel<T, R, E> decoratedChannel) {
		this.decoratedChannel = requireNonNull(decoratedChannel,
				"The argument 'decoratedChannel' must not be null."); //$NON-NLS-1$
	}

	@Override
	public int nrOfWaitingTasks() {
		return decoratedChannel.nrOfWaitingTasks();
	}

	@Override
	public String taskName() {
		return decoratedChannel.taskName();
	}

	@Override
	public void sendTask(T task) {
		decoratedChannel.sendTask(task);
	}

	@Override
	public T getTask() throws InterruptedException {
		return decoratedChannel.getTask();
	}

	@Override
	public Stream<T> drainTasks() {
		return decoratedChannel.drainTasks();
	}

	@Override
	public void sendResult(R result) throws InterruptedException {
		decoratedChannel.sendResult(result);
	}

	@Override
	public Stream<R> drainResults() {
		return decoratedChannel.drainResults();
	}

	@Override
	public void sendError(E error) throws InterruptedException {
		decoratedChannel.sendError(error);
	}

	@Override
	public Stream<E> drainErrors() {
		return decoratedChannel.drainErrors();
	}
	
}
