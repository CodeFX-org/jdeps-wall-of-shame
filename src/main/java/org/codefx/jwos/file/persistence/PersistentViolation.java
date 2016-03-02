package org.codefx.jwos.file.persistence;// NOT_PUBLISHED

import org.codefx.jwos.jdeps.dependency.Violation;

import java.util.List;

import static org.codefx.jwos.Util.transformToImmutableSet;
import static org.codefx.jwos.Util.transformToList;

public class PersistentViolation {
	
	public PersistentType dependent;
	public List<PersistentInternalType> internalDependencies;
	
	public static PersistentViolation from(Violation violation) {
		PersistentViolation persistent = new PersistentViolation();
		persistent.dependent = PersistentType.from(violation.getDependent());
		persistent.internalDependencies = transformToList(
				violation.getInternalDependencies(), PersistentInternalType::from);
		return persistent;
	}
	
	public Violation toViolation() {
		return Violation.buildFor(
				dependent.toType(),
				transformToImmutableSet(internalDependencies, PersistentInternalType::toType)
		);
	}
	
}
