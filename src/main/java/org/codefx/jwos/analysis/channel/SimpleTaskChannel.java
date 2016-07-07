package org.codefx.jwos.analysis.channel;

import com.google.common.collect.Iterables;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.StreamSupport.stream;

class SimpleTaskChannel<T, R, E> implements TaskChannel<T,R,E> {

	private final String taskName;

	private final BlockingQueue<T> tasks;
	private final BlockingQueue<R> results;
	private final BlockingQueue<E> errors;

	public SimpleTaskChannel(String taskName, int resultCapacity, int errorCapacity) {
		this.taskName = requireNonNull(taskName, "The argument 'taskName' must not be null.");
		tasks = new LinkedBlockingQueue<>();
		results = createQueue(resultCapacity);
		errors = createQueue(errorCapacity);
	}

	private static <E> BlockingQueue<E> createQueue(int capacity) {
		return capacity == 0 ? new LinkedBlockingQueue<>() : new ArrayBlockingQueue<>(capacity);
	}

	@Override
	public int nrOfWaitingTasks() {
		return tasks.size();
	}

	@Override
	public boolean noWaitingTasks() {
		return tasks.isEmpty();
	}

	@Override
	public String taskName() {
		return taskName;
	}

	@Override
	public void sendTask(T task) {
		tasks.add(task);
	}

	@Override
	public T getTask() throws InterruptedException {
		return tasks.take();
	}

	@Override
	public Stream<T> drainTasks() {
		// create an iterable that empties 'tasks' as it returns elements
		return stream(Iterables.consumingIterable(tasks).spliterator(), false);
	}

	@Override
	public void sendResult(R result) throws InterruptedException {
		results.add(result);
	}

	@Override
	public Stream<R> drainResults() {
		// create an iterable that empties 'results' as it returns elements
		return stream(Iterables.consumingIterable(results).spliterator(), false);
	}

	@Override
	public void sendError(E error) throws InterruptedException {
		errors.add(error);
	}

	@Override
	public Stream<E> drainErrors() {
		// create an iterable that empties 'errors' as it returns elements
		return stream(Iterables.consumingIterable(errors).spliterator(), false);
	}

}
