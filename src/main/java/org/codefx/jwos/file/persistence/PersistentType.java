package org.codefx.jwos.file.persistence;// NOT_PUBLISHED

import org.codefx.jwos.jdeps.dependency.Type;

public class PersistentType {

	public String packageName;
	public String className;

	public static PersistentType from(Type type) {
		PersistentType persistent = new PersistentType();
		persistent.packageName = type.getPackageName();
		persistent.className = type.getClassName();
		return persistent;
	}

	public Type toType() {
		return Type.of(packageName, className);
	}

}
