package jadx.core.dex.attributes;

public interface ILineAttributeNode {
	int getSourceLine();

	void setSourceLine(int sourceLine);

	int getDecompiledLine();

	void setDecompiledLine(int line);

	int getDefPosition();

	void setDefPosition(int pos);
}
