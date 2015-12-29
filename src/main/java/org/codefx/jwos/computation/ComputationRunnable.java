package org.codefx.jwos.computation;

import static java.util.Objects.requireNonNull;

public class ComputationRunnable implements Runnable {

	private final Computation computation;
	private boolean aborted;

	public ComputationRunnable(Computation computation) {
		this.computation = requireNonNull(computation, "The argument 'computation' must not be null.");
		this.aborted = false;
	}

	@Override
	public void run() {
		while (!aborted) {
			try {
				computation.compute();
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				aborted = true;
			}
		}
	}

	public void abort() {
		aborted = true;
	}

}
