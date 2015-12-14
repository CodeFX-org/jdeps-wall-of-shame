package org.codefx.jwos.connect;

import java.util.Collection;

import static java.util.Objects.requireNonNull;

public class TransformerToMany<I, O> {

	private final BlockingSender<I> input;
	private final ThrowingFunction<I, Collection<O>> function;
	private final BlockingReceiver<O> output;

	public TransformerToMany(
			BlockingSender<I> input, ThrowingFunction<I, Collection<O>> function, BlockingReceiver<O> output) {
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
		Collection<O> outs = function.apply(in);
		for (O out : outs)
			output.acceptNext(out);
	}
}
