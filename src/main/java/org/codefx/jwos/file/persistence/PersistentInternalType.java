package org.codefx.jwos.file.persistence;// NOT_PUBLISHED

import org.codefx.jwos.jdeps.dependency.InternalType;

public class PersistentInternalType extends PersistentType {

	public String category;
	public String source;

	public static PersistentInternalType from(InternalType type) {
		PersistentInternalType persistent = new PersistentInternalType();
		persistent.packageName = type.getPackageName();
		persistent.className = type.getClassName();
		persistent.category = type.getCategory();
		persistent.source = type.getSource();
		return persistent;
	}

	public InternalType toType() {
		return InternalType.of(packageName, className, category, source);
	}

}
