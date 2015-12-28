package org.codefx.jwos.analysis;

import com.google.common.collect.Iterables;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.StreamSupport.stream;

class TaskChannel<T, R, E> {

	private final String taskName;

	private final BlockingQueue<T> tasks;
	private final BlockingQueue<R> results;
	private final BlockingQueue<E> errors;

	/**
	 * Creates a new channel with the specified capacities for results and errors (where 0 means unbounded).
	 */
	public TaskChannel(String taskName, int resultCapacity, int errorCapacity) {
		this.taskName = requireNonNull(taskName, "The argument 'taskName' must not be null.");
		tasks = new LinkedBlockingQueue<>();
		results = createQueue(resultCapacity);
		errors = createQueue(errorCapacity);
	}

	private static <E> BlockingQueue<E> createQueue(int capacity) {
		return capacity == 0 ? new LinkedBlockingQueue<>() : new ArrayBlockingQueue<>(capacity);
	}

	/**
	 * Creates a new channel with the specified capacity for results and errors, respectively.
	 */
	public TaskChannel(String taskName, int capacity) {
		this(taskName, capacity, capacity);
	}

	/**
	 * Creates a new channel with unbounded capacities for results and errors.
	 *
	 * @param taskName
	 */
	public TaskChannel(String taskName) {
		this(taskName, 0, 0);
	}

	public int nrOfWaitingTasks() {
		return tasks.size();
	}

	public String taskName() {
		return taskName;
	}

	public void sendTask(T task) {
		tasks.add(task);
	}

	public T getTask() throws InterruptedException {
		return tasks.take();
	}

	public void sendResult(R result) throws InterruptedException {
		results.add(result);
	}

	public Stream<R> drainResults() {
		return stream(Iterables.consumingIterable(results).spliterator(), false);
	}

	public void sendError(E error) throws InterruptedException {
		errors.add(error);
	}

	public Stream<E> drainErrors() {
		return stream(Iterables.consumingIterable(errors).spliterator(), false);
	}

}
