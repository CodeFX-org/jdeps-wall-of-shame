package org.codefx.jwos.analysis.channel;

import java.util.Collection;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;

/**
 * A channel handles communication of a single type of tasks.
 * <p>
 * It consists of three blocking queue, one to send out tasks and two to receive results or failures, respectively.
 *
 * @param <T> the type of tasks
 * @param <R> the type of the tasks' successful result 
 * @param <E> the type of the tasks' error
 */
public interface TaskChannel<T, R, E> {

	// CREATION
	
	/**
	 * Creates a new channel with unbounded capacities for results and errors.
	 */
	static <T, R, E> TaskChannel<T, R, E> namedAndUnbounded(String taskName) {
		return new SimpleTaskChannel<>(taskName, 0, 0);
	}

	/**
	 * Creates a new channel with the specified capacity for results and errors, respectively (where 0 means unbounded).
	 */
	static <T, R, E> TaskChannel<T, R, E> namedAndBounded(String taskName, int capacity) {
		return new SimpleTaskChannel<>(taskName, capacity, capacity);
	}

	/**
	 * Creates a new channel with the specified capacities for results and errors (where 0 means unbounded).
	 */
	static <T, R, E> TaskChannel<T, R, E> namedAndBounded(String taskName, int resultCapacity, int errorCapacity) {
		return new SimpleTaskChannel<>(taskName, resultCapacity, errorCapacity);
	}

	/**
	 * Creates a new channel that replays the specified tasks, results, and errors before querying this channel. 
	 */
	default TaskChannel<T, R, E> replaying(Collection<T> tasks, Collection<R> results, Collection<E> errors) {
		return new ReplayingTaskChannelDecorator<>(this, tasks, results, errors);
	}

	/**
	 * Creates a new channel that replays the specified results and errors before querying this channel. 
	 */
	default TaskChannel<T, R, E> replaying(Collection<R> results, Collection<E> errors) {
		return new ReplayingTaskChannelDecorator<>(this, emptySet(), results, errors);
	}

	/**
	 * Creates a new channel that sends all tasks, results, and errors to this one and to the specified one.
	 * Only this channel will be queried for items.
	 */
	default TaskChannel<T, R, E> spy(TaskChannel<T, R, E> listeningChannel) {
		return new SpyingTaskChannelDecorator<>(this, listeningChannel);
	}

	// CONTRACT

	int nrOfWaitingTasks();

	String taskName();

	void sendTask(T task);

	T getTask() throws InterruptedException;

	void sendResult(R result) throws InterruptedException;

	Stream<R> drainResults();

	void sendError(E error) throws InterruptedException;

	Stream<E> drainErrors();
}
