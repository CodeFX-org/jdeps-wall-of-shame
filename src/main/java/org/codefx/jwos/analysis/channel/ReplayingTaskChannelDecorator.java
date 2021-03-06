package org.codefx.jwos.analysis.channel;

import com.google.common.collect.Iterables;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Stream.concat;
import static java.util.stream.StreamSupport.stream;

/**
 * A decorator for a {@link TaskChannel} that will replay the tasks, results and errors specified during construction.
 * <p>
 * Query methods will first exhaust the specified elements before delegating to the decorated channel.
 * All sending methods remain unchanged.
 */
class ReplayingTaskChannelDecorator<T, R, E> extends AbstractTaskChannelDecorator<T, R, E> {

	private final BlockingQueue<T> tasksToReplay;
	private final BlockingQueue<R> resultsToReplay;
	private final BlockingQueue<E> errorsToReplay;

	public ReplayingTaskChannelDecorator(
			TaskChannel<T, R, E> decoratedChannel,
			Collection<T> tasksToReplay,
			Collection<R> resultsToReplay,
			Collection<E> errorsToReplay) {
		super(decoratedChannel);
		requireNonNull(tasksToReplay, "The argument 'tasksToReplay' must not be null.");
		requireNonNull(resultsToReplay, "The argument 'resultsToReplay' must not be null.");
		requireNonNull(errorsToReplay, "The argument 'errorsToReplay' must not be null.");
		this.tasksToReplay = new LinkedBlockingQueue<>(tasksToReplay);
		this.resultsToReplay = new LinkedBlockingQueue<>(resultsToReplay);
		this.errorsToReplay = new LinkedBlockingQueue<>(errorsToReplay);
	}

	@Override
	public int nrOfWaitingTasks() {
		return tasksToReplay.size() + super.nrOfWaitingTasks();
	}

	@Override
	public boolean noWaitingTasks() {
		return tasksToReplay.isEmpty() && super.noWaitingTasks();
	}

	@Override
	public T getTask() throws InterruptedException {
		T task = tasksToReplay.poll();
		return task == null
				? super.getTask()
				: task;
	}

	@Override
	public Stream<T> drainTasks() {
		Stream<T> drainReplay = stream(Iterables.consumingIterable(tasksToReplay).spliterator(), false);
		return concat(drainReplay, super.drainTasks());
	}

	@Override
	public Stream<R> drainResults() {
		Stream<R> drainReplay = stream(Iterables.consumingIterable(resultsToReplay).spliterator(), false);
		return concat(drainReplay, super.drainResults());
	}

	@Override
	public Stream<E> drainErrors() {
		Stream<E> drainReplay = stream(Iterables.consumingIterable(errorsToReplay).spliterator(), false);
		return concat(drainReplay, super.drainErrors());
	}

}
