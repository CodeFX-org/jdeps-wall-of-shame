package org.codefx.jwos.connect;

import static java.util.Objects.requireNonNull;

public class Sink<I> {

	private final BlockingSender<I> input;
	private final ThrowingConsumer<I> consumer;

	public Sink(BlockingSender<I> input, ThrowingConsumer<I> consumer) {
		this.input = requireNonNull(input, "The argument 'input' must not be null.");
		this.consumer = requireNonNull(consumer, "The consumer 'function' must not be null.");
	}

	public void consume() {
		boolean aborted = false;
		while (!aborted)
			try {
				consumeNext();
			} catch (InterruptedException ex) {
				// TODO log exception
				aborted = true;
			} catch (Exception ex) {
				// TODO log exception
			}
	}

	private void consumeNext() throws Exception {
		I in = input.getNext();
		consumer.consume(in);
	}
}
