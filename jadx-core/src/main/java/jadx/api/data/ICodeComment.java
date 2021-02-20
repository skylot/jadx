package jadx.api.data;

public interface ICodeComment {

	IJavaNodeRef getNodeRef();

	String getComment();

	int getOffset();
}
