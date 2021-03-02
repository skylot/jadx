package jadx.api;

import java.util.List;

public interface JavaNode {

	String getName();

	String getFullName();

	JavaClass getDeclaringClass();

	JavaClass getTopParentClass();

	int getDecompiledLine();

	int getDefPos();

	List<JavaNode> getUseIn();
}
