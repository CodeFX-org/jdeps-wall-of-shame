package org.codefx.jwos.connect;

@FunctionalInterface
public interface BlockingReceiver<M> {

	void acceptNext(M message) throws InterruptedException;

}
