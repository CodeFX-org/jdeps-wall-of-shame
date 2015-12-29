package org.codefx.jwos.computation;

@FunctionalInterface
public interface SendResult<R> {

	void send(R result) throws InterruptedException;

}
