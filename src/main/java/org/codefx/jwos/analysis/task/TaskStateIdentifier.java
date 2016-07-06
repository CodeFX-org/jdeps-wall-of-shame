package org.codefx.jwos.analysis.task;

import static java.lang.String.format;

public enum TaskStateIdentifier {

	NOT_COMPUTED,
	QUEUED,
	STARTED,
	FAILED,
	SUCCEEDED;

	public boolean isFinished() {
		switch (this) {
			case NOT_COMPUTED:
			case QUEUED:
			case STARTED:
				return false;
			case FAILED:
			case SUCCEEDED:
				return true;
			default:
				throw new IllegalArgumentException(format("Unknown task state \"%s\".", this));
		}
	}

}
