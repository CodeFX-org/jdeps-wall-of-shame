package org.codefx.jwos.connect;

import org.slf4j.Logger;

import static java.util.Objects.requireNonNull;

public class Sink<I> implements Runnable {

	private final BlockingSender<I> input;
	private final ThrowingConsumer<I> consumer;
	private final Logger logger;

	public Sink(BlockingSender<I> input, ThrowingConsumer<I> consumer, Logger logger) {
		this.input = requireNonNull(input, "The argument 'input' must not be null.");
		this.consumer = requireNonNull(consumer, "The consumer 'function' must not be null.");
		this.logger = requireNonNull(logger, "The argument 'logger' must not be null.");
	}

	@Override
	public void run() {
		consume();
	}

	public void consume() {
		boolean aborted = false;
		while (!aborted)
			try {
				consumeNext();
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				logger.warn("Interruption while waiting to consume.", ex);
				aborted = true;
			} catch (Exception ex) {
				logger.error("Error while consuming.", ex);
			}
	}

	private void consumeNext() throws Exception {
		I in = input.getNext();
		consumer.consume(in);
	}
}
