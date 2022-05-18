package jadx.api.metadata;

public interface ICodeNodeRef extends ICodeAnnotation {
	int getDefPosition();

	void setDefPosition(int pos);
}
