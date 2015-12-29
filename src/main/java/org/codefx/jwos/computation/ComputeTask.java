package org.codefx.jwos.computation;

@FunctionalInterface
public interface ComputeTask<T, R> {

	R compute(T task) throws Exception;

}
