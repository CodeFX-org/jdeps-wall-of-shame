package org.codefx.jwos.connect;

@FunctionalInterface
public interface ThrowingSupplier<O> {

	O supply() throws Exception;

}
