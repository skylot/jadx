package jadx.api;

import java.util.List;

import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeNodeRef;

public interface JavaNode {

	ICodeNodeRef getCodeNodeRef();

	String getName();

	String getFullName();

	JavaClass getDeclaringClass();

	JavaClass getTopParentClass();

	int getDefPos();

	List<JavaNode> getUseIn();

	void removeAlias();

	boolean isOwnCodeAnnotation(ICodeAnnotation ann);
}
