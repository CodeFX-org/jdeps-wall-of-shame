package org.codefx.jwos.analysis.channel;

import java.util.stream.Stream;

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
	
	int nrOfWaitingTasks();

	String taskName();

	void sendTask(T task);

	T getTask() throws InterruptedException;

	void sendResult(R result) throws InterruptedException;

	Stream<R> drainResults();

	void sendError(E error) throws InterruptedException;

	Stream<E> drainErrors();
}
