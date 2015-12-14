package org.codefx.jwos.connect;

@FunctionalInterface
public interface ThrowingConsumer<I> {

	void consume(I input) throws Exception;

	static <I> ThrowingConsumer<I> ignore() {
		return in -> {
			// do nothing
		};
	}

}
