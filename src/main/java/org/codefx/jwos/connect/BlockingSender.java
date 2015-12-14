package org.codefx.jwos.connect;

@FunctionalInterface
public interface BlockingSender<M> {

	M getNext() throws InterruptedException;

}
