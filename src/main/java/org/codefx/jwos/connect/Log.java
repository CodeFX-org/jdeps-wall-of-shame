package org.codefx.jwos.connect;

import static java.lang.String.format;

public class Log {

	// TODO use logging framework

	public static <O> ThrowingSupplier<O> log(ThrowingSupplier<O> supplier, String outFormat) {
		return () -> {
			O out = supplier.supply();
			if (!outFormat.isEmpty())
				System.out.println(format(outFormat, out));
			return out;
		};
	}

	public static <I, O> ThrowingFunction<I, O> log(
			String inFormat, ThrowingFunction<I, O> function, String outFormat) {
		return in -> {
			if (!inFormat.isEmpty())
				System.out.println(format(inFormat, in));
			O out = function.apply(in);
			if (!outFormat.isEmpty())
				System.out.println(format(outFormat, out));
			return out;
		};
	}

	public static <I> ThrowingConsumer<I> log(String inFormat, ThrowingConsumer<I> consumer) {
		return in -> {
			if (!inFormat.isEmpty())
				System.out.println(format(inFormat, in));
			consumer.consume(in);
		};
	}

}
