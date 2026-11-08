package jadx.api.gui.tree;

public interface ITreeNodeDetails {
	String makeDescString();

	boolean hasDescString();

	String makeStringHtml();

	String makeLongStringHtml();

	boolean disableHtml();
}
