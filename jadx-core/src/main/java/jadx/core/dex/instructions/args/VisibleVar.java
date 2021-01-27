package jadx.core.dex.instructions.args;

public interface VisibleVar {
	int getIndex();

	void setIndex(int index);

	String getName();

	void setName(String name);

	ArgType getType();

}
