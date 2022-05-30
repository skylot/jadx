package jadx.api.data;

public interface IJavaNodeRef extends Comparable<IJavaNodeRef> {

	public enum RefType {
		CLASS, FIELD, METHOD, PKG
	}

	RefType getType();

	String getDeclaringClass();

	String getShortId();
}
