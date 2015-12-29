package org.codefx.jwos.computation;

@FunctionalInterface
public interface ReceiveTask<T> {

	T receive() throws InterruptedException;

}
