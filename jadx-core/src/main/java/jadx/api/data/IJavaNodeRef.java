package jadx.api.data;

public interface IJavaNodeRef {

	enum RefType {
		CLASS, FIELD, METHOD
	}

	RefType getType();

	String getDeclaringClass();

	String getShortId();
}
