package jadx.api;

public interface JavaNode {

	String getName();

	String getFullName();

	JavaClass getDeclaringClass();
}
