package jadx.api.data;

import org.jetbrains.annotations.Nullable;

public interface ICodeRename extends Comparable<ICodeRename> {

	IJavaNodeRef getNodeRef();

	@Nullable
	IJavaCodeRef getCodeRef();

	String getNewName();
}
