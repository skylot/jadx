package jadx.api;

import java.util.List;

import jadx.api.metadata.ICodeAnnotation;

public interface JavaNode {

	String getName();

	String getFullName();

	JavaClass getDeclaringClass();

	JavaClass getTopParentClass();

	int getDefPos();

	List<JavaNode> getUseIn();

	default void removeAlias() {
	}

	boolean isOwnCodeAnnotation(ICodeAnnotation ann);
}
