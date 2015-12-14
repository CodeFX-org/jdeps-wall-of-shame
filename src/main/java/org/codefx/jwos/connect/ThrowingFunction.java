package org.codefx.jwos.connect;

@FunctionalInterface
public interface ThrowingFunction<I, O> {

	O apply(I input) throws Exception;

}
