package org.codefx.jwos.computation;// NOT_PUBLISHED


import static java.util.Objects.requireNonNull;

public class RecurrentComputation implements Computation {

	private final String name;
	private final Compute compute;
	private final int sleepTimeInMs;

	public RecurrentComputation(String name, Compute compute, int sleepTimeInMs) {
		this.name = name;
		this.sleepTimeInMs = sleepTimeInMs;
		this.compute = requireNonNull(compute, "The argument 'compute' must not be null.");
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public void compute() throws InterruptedException {
		try {
			compute.compute();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
