package org.codefx.jwos.connect;

import org.slf4j.Logger;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class Source<O> {

	private final ThrowingSupplier<Optional<O>> supplier;
	private final BlockingReceiver<O> output;
	private final Logger logger;

	public Source(ThrowingSupplier<Optional<O>> supplier, BlockingReceiver<O> output, Logger logger) {
		this.supplier = requireNonNull(supplier, "The argument 'supplier' must not be null.");
		this.output = requireNonNull(output, "The argument 'output' must not be null.");
		this.logger = requireNonNull(logger, "The argument 'logger' must not be null.");
	}

	public void supply() {
		boolean exhausted = false;
		boolean aborted = false;
		while (!exhausted && !aborted)
			try {
				exhausted = supplyNext();
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				logger.warn("Interruption while waiting to supply.", ex);
				aborted = true;
			} catch (Exception ex) {
				logger.error("Error while supplying.", ex);
			}
	}

	private boolean supplyNext() throws Exception {
		Optional<O> out = supplier.supply();
		if (out.isPresent())
			output.acceptNext(out.get());
		return out.isPresent();
	}
}
