package org.codefx.jwos.connect;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class Source<O> {

	private final ThrowingSupplier<Optional<O>> supplier;
	private final BlockingReceiver<O> output;

	public Source(ThrowingSupplier<Optional<O>> supplier, BlockingReceiver<O> output) {
		this.supplier = requireNonNull(supplier, "The argument 'supplier' must not be null.");
		this.output = requireNonNull(output, "The argument 'output' must not be null.");
	}

	public void supply() {
		boolean exhausted = false;
		boolean aborted = false;
		while (!exhausted && !aborted)
			try {
				exhausted = supplyNext();
			} catch (InterruptedException ex) {
				// TODO log exception
				aborted = true;
			} catch (Exception ex) {
				// TODO log exception
			}
	}

	private boolean supplyNext() throws Exception {
		Optional<O> out = supplier.supply();
		if (out.isPresent())
			output.acceptNext(out.get());
		return out.isPresent();
	}
}
