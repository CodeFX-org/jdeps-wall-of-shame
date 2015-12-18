package org.codefx.jwos.connect;

// TODO The semantics of sender and receiver are screwed up (in my head as well as in code). Replace with input, output?

@FunctionalInterface
public interface BlockingSender<M> {

	M getNext() throws InterruptedException;

}
