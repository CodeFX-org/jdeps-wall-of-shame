package org.codefx.jwos.computation;

@FunctionalInterface
public interface SendError<T> {

	void send(T task, Exception error) throws InterruptedException;

}
