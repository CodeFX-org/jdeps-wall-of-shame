package org.codefx.jwos.analysis.task;

/**
 * The state a task is in; implementations are immutable.
 *
 * @param <R> the task's result if it succeeded
 */
interface TaskState<R> {

	TaskStateIdentifier identifier();

	default boolean isFinished() {
		return identifier().isFinished();
	}

	TaskState<R> queued();

	TaskState<R> started();

	TaskState<R> failed(Exception exception);

	TaskState<R> succeeded(R result);

}
