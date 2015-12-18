package org.codefx.jwos.connect;

import org.slf4j.Logger;

import static java.util.Objects.requireNonNull;

public class Transformer<I, O> implements Runnable {

	private final BlockingSender<I> input;
	private final ThrowingFunction<I, O> function;
	private final BlockingReceiver<O> output;
	private final Logger logger;

	public Transformer(
			BlockingSender<I> input, ThrowingFunction<I, O> function, BlockingReceiver<O> output, Logger logger) {
		this.input = requireNonNull(input, "The argument 'input' must not be null.");
		this.function = requireNonNull(function, "The argument 'function' must not be null.");
		this.output = requireNonNull(output, "The argument 'output' must not be null.");
		this.logger = requireNonNull(logger, "The argument 'logger' must not be null.");
	}

	@Override
	public void run() {
		transform();
	}

	public void transform() {
		boolean aborted = false;
		while (!aborted)
			try {
				transformNext();
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				logger.warn("Interruption while waiting to take or put.", ex);
				aborted = true;
			} catch (Exception ex) {
				logger.error("Error while transforming.", ex);
			}
	}

	private void transformNext() throws Exception {
		I in = input.getNext();
		O out = function.apply(in);
		output.acceptNext(out);
	}
}
