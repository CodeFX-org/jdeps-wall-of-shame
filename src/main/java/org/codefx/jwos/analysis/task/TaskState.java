package org.codefx.jwos.analysis.task;

interface TaskState<R> {

	TaskStateIdentifier identifier();

	TaskState<R> queued();

	TaskState<R> started();

	TaskState<R> failed(Exception exception);

	TaskState<R> succeeded(R result);

}
