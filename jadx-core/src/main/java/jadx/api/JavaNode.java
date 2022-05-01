package jadx.api;

import java.util.List;

public interface JavaNode {

	String getName();

	String getFullName();

	JavaClass getDeclaringClass();

	JavaClass getTopParentClass();

	int getDefPos();

	List<JavaNode> getUseIn();

	default void removeAlias() {
	}
}
