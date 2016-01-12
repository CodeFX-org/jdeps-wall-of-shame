package org.codefx.jwos.computation;

/**
 * Encapsulates the computation of an individual task.
 * <p>
 * This can go along the lines of "receive a task, try to compute it, send result or error" but it doesn't have to.
 * It is possible that a computation only creates or completes tasks without doing the other.
 */
public interface Computation {

	String name();

	void compute() throws InterruptedException;

}
