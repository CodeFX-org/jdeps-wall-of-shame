package org.codefx.jwos.artifact;

public enum InternalDependencies {

	NONE,
	INDIRECT,
	DIRECT;

	public InternalDependencies combineWithDependee(InternalDependencies other) {
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
