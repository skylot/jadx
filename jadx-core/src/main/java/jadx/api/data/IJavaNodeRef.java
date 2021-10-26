package jadx.api.data;

public interface IJavaNodeRef extends Comparable<IJavaNodeRef> {

	enum RefType {
		CLASS, FIELD, METHOD, PKG
	}

	RefType getType();

	String getDeclaringClass();

	String getShortId();
}
