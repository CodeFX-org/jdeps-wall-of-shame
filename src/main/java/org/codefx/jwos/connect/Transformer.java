package org.codefx.jwos.connect;

import static java.util.Objects.requireNonNull;

public class Transformer<I, O> {

	private final BlockingSender<I> input;
	private final ThrowingFunction<I, O> function;
	private final BlockingReceiver<O> output;

	public Transformer(BlockingSender<I> input, ThrowingFunction<I, O> function, BlockingReceiver<O> output) {
		this.input = requireNonNull(input, "The argument 'input' must not be null.");
		this.function = requireNonNull(function, "The argument 'function' must not be null.");
		this.output = requireNonNull(output, "The argument 'output' must not be null.");
	}

	public void transform() {
		boolean aborted = false;
		while (!aborted)
			try {
				transformNext();
			} catch (InterruptedException ex) {
				// TODO log exception
				aborted = true;
			} catch (Exception ex) {
				// TODO log exception
			}
	}

	private void transformNext() throws Exception {
		I in = input.getNext();
		O out = function.apply(in);
		output.acceptNext(out);
	}
}
