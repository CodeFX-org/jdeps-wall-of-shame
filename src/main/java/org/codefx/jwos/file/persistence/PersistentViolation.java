package org.codefx.jwos.file.persistence;// NOT_PUBLISHED

import org.codefx.jwos.jdeps.dependency.Violation;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class PersistentViolation {
	
	public PersistentType dependent;
	public List<PersistentInternalType> internalDependencies;
	
	public static PersistentViolation from(Violation violation) {
		PersistentViolation persistent = new PersistentViolation();
		persistent.dependent = PersistentType.from(violation.getDependent());
		persistent.internalDependencies = violation
				.getInternalDependencies().stream()
				.map(PersistentInternalType::from)
				.collect(toList());
		return persistent;
	}
	
	public Violation toViolation() {
		return Violation.buildFor(
				dependent.toType(),
				internalDependencies.stream()
						.map(PersistentInternalType::toType)
						.collect(toSet())
		);
	}
	
}
