package org.codefx.jwos.computation;

import static java.util.Objects.requireNonNull;

public class ComputationThread extends Thread {

	private final Computation computation;
	private final OnAbort onAbort;
	private boolean aborted;

	public ComputationThread(Computation computation) {
		this(computation, OnAbort.INTERRUPT_THREAD);
	}

	public ComputationThread(Computation computation, OnAbort onAbort) {
		super(computation.name());
		this.computation = requireNonNull(computation, "The argument 'computation' must not be null.");
		this.onAbort = onAbort;
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

	/**
	 * Informs the thread that it should abort at a convenient moment.
	 */
	public void notifyAbort() {
		aborted = true;
		if (onAbort == OnAbort.INTERRUPT_THREAD)
			this.interrupt();
	}

	public enum OnAbort {
		DO_NOT_INTERRUPT_THREAD, INTERRUPT_THREAD;
	}

}
