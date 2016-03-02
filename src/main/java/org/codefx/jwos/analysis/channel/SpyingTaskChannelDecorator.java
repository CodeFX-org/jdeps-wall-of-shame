package org.codefx.jwos.analysis.channel;

import static java.util.Objects.requireNonNull;

/**
 * A decorator for a {@link TaskChannel} that will spy on it and send all tasks, results and errors
 * into another channel specified during construction (queries will be forwarded to the decorated channel). 
 * <p>
 * This should not be confused with a Y-Piece which would likely send tasks to two channels (like this one)
 * but also receive results/errors from both to join the responses into a single channel.
 * <p>
 * The order in which the decorated and listening channels are called is unspecified and should not be relied
 * upon. Note that a blocking listening channel might introduce unexpected waits.  
 */
class SpyingTaskChannelDecorator<T, R, E> extends AbstractTaskChannelDecorator<T, R, E> {

	private final TaskChannel<T, R, E> listeningChannel;

	SpyingTaskChannelDecorator(TaskChannel<T, R, E> decoratedChannel, TaskChannel<T, R, E> listeningChannel) {
		super(decoratedChannel);
		this.listeningChannel = requireNonNull(listeningChannel, "The argument 'listeningChannel' must not be null.");
	}

	@Override
	public void sendTask(T task) {
		super.sendTask(task);
		listeningChannel.sendTask(task);
	}

	@Override
	public void sendResult(R result) throws InterruptedException {
		super.sendResult(result);
		listeningChannel.sendResult(result);
	}

	@Override
	public void sendError(E error) throws InterruptedException {
		super.sendError(error);
		listeningChannel.sendError(error);
	}
}
