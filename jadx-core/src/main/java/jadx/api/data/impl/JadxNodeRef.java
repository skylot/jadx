package jadx.api.data.impl;

import java.util.Comparator;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.api.data.IJavaNodeRef;

public class JadxNodeRef implements IJavaNodeRef {

	@Nullable
	public static JadxNodeRef forJavaNode(JavaNode javaNode) {
		if (javaNode instanceof JavaClass) {
			return forCls((JavaClass) javaNode);
		}
		if (javaNode instanceof JavaMethod) {
			return forMth((JavaMethod) javaNode);
		}
		if (javaNode instanceof JavaField) {
			return forFld((JavaField) javaNode);
		}
		return null;
	}

	public static JadxNodeRef forCls(JavaClass cls) {
		return new JadxNodeRef(RefType.CLASS, getClassRefStr(cls), null);
	}

	public static JadxNodeRef forCls(String clsFullName) {
		return new JadxNodeRef(RefType.CLASS, clsFullName, null);
	}

	public static JadxNodeRef forMth(JavaMethod mth) {
		return new JadxNodeRef(RefType.METHOD,
				getClassRefStr(mth.getDeclaringClass()),
				mth.getMethodNode().getMethodInfo().getShortId());
	}

	public static JadxNodeRef forFld(JavaField fld) {
		return new JadxNodeRef(RefType.FIELD,
				getClassRefStr(fld.getDeclaringClass()),
				fld.getFieldNode().getFieldInfo().getShortId());
	}

	public static JadxNodeRef forPkg(String pkgFullName) {
		return new JadxNodeRef(RefType.PKG, pkgFullName, "");
	}

	private static String getClassRefStr(JavaClass cls) {
		return cls.getClassNode().getClassInfo().getRawName();
	}

	private RefType refType;
	private String declClass;
	@Nullable
	private String shortId;

	public JadxNodeRef(RefType refType, String declClass, @Nullable String shortId) {
		this.refType = refType;
		this.declClass = declClass;
		this.shortId = shortId;
	}

	public JadxNodeRef() {
		// for json deserialization
	}

	@Override
	public RefType getType() {
		return refType;
	}

	public void setRefType(RefType refType) {
		this.refType = refType;
	}

	@Override
	public String getDeclaringClass() {
		return declClass;
	}

	public void setDeclClass(String declClass) {
		this.declClass = declClass;
	}

	@Nullable
	@Override
	public String getShortId() {
		return shortId;
	}

	public void setShortId(@Nullable String shortId) {
		this.shortId = shortId;
	}

	private static final Comparator<IJavaNodeRef> COMPARATOR = Comparator
			.comparing(IJavaNodeRef::getType)
			.thenComparing(IJavaNodeRef::getDeclaringClass)
			.thenComparing(IJavaNodeRef::getShortId);

	@Override
	public int compareTo(@NotNull IJavaNodeRef other) {
		return COMPARATOR.compare(this, other);
	}

	@Override
	public int hashCode() {
		return Objects.hash(refType, declClass, shortId);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof JadxNodeRef)) {
			return false;
		}
		JadxNodeRef that = (JadxNodeRef) o;
		return refType == that.refType
				&& Objects.equals(declClass, that.declClass)
				&& Objects.equals(shortId, that.shortId);
	}

	@Override
	public String toString() {
		switch (refType) {
			case CLASS:
			case PKG:
				return declClass;
			case FIELD:
			case METHOD:
				return declClass + "->" + shortId;
			default:
				return "unknown node ref type";
		}
	}
}
