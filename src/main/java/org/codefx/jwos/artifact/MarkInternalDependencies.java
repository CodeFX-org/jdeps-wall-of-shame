package org.codefx.jwos.artifact;

public enum MarkInternalDependencies {

	NONE,
	INDIRECT,
	DIRECT;

	public MarkInternalDependencies combineWithDependee(MarkInternalDependencies other) {
		if (this == NONE) {
			switch (other) {
				case NONE:
					return NONE;
				case INDIRECT:
					return INDIRECT;
				case DIRECT:
					return INDIRECT;
			}
		}

		return this;
	}

}
