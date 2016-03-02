package org.codefx.jwos.file.persistence;// NOT_PUBLISHED

import org.codefx.jwos.jdeps.dependency.Type;

public class PersistentType {

	public String className;

	public static PersistentType from(Type type) {
		PersistentType persistent = new PersistentType();
		persistent.className = type.getPackageName() + "." + type.getClassName();
		return persistent;
	}

	public Type toType() {
		return Type.of(className);
	}

}
