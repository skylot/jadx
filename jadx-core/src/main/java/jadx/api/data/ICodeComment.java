package jadx.api.data;

import org.jetbrains.annotations.Nullable;

public interface ICodeComment extends Comparable<ICodeComment> {

	IJavaNodeRef getNodeRef();

	@Nullable
	IJavaCodeRef getCodeRef();

	String getComment();

	CommentStyle getStyle();
}
