package jadx.core.dex.attributes;

public interface ILineAttributeNode {
	int getSourceLine();

	void setSourceLine(int sourceLine);

	int getDefPosition();

	void setDefPosition(int pos);
}
