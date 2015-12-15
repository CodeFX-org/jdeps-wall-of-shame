package org.codefx.jwos.connect;

import org.slf4j.Logger;

import static java.lang.String.format;

public class Log {

	public static <O> ThrowingSupplier<O> log(ThrowingSupplier<O> supplier, String outFormat, Logger logger) {
		return () -> {
			O out = supplier.supply();
			if (!outFormat.isEmpty())
				logger.info(format(outFormat, out));
			return out;
		};
	}

	public static <I, O> ThrowingFunction<I, O> log(
			String inFormat, ThrowingFunction<I, O> function, String outFormat, Logger logger) {
		return in -> {
			if (!inFormat.isEmpty())
				logger.info(format(inFormat, in));
			O out = function.apply(in);
			if (!outFormat.isEmpty())
				logger.info(format(outFormat, out));
			return out;
		};
	}

	public static <I> ThrowingConsumer<I> log(String inFormat, ThrowingConsumer<I> consumer, Logger logger) {
		return in -> {
			if (!inFormat.isEmpty())
				logger.info(format(inFormat, in));
			consumer.consume(in);
		};
	}

}
