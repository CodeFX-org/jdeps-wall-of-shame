package org.codefx.jwos.file;

import java.io.IOException;

public class RuntimeIOException extends RuntimeException {

	public RuntimeIOException(IOException cause) {
		super(cause);
	}

	@Override
	public synchronized IOException getCause() {
		return (IOException) super.getCause();
	}
}
