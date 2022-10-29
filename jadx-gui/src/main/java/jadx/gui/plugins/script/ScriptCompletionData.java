package jadx.gui.plugins.script;

import javax.swing.Icon;
import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;

public class ScriptCompletionData implements Completion {

	private final CompletionProvider provider;
	private final int relevance;

	private String input;
	private String code;
	private int replacePos;
	private Icon icon;
	private String summary;
	private String toolTip;

	public ScriptCompletionData(CompletionProvider provider, int relevance) {
		this.provider = provider;
		this.relevance = relevance;
	}

	public void setData(String input, String code, int replacePos) {
		this.input = input;
		this.code = code;
		this.replacePos = replacePos;
	}

	@Override
	public String getInputText() {
		return input;
	}

	@Override
	public CompletionProvider getProvider() {
		return provider;
	}

	@Override
	public String getAlreadyEntered(JTextComponent comp) {
		return provider.getAlreadyEnteredText(comp);
	}

	@Override
	public int getRelevance() {
		return relevance;
	}

	@Override
	public String getReplacementText() {
		return code.substring(0, replacePos) + input;
	}

	@Override
	public Icon getIcon() {
		return icon;
	}

	public void setIcon(Icon icon) {
		this.icon = icon;
	}

	@Override
	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	@Override
	public String getToolTipText() {
		return toolTip;
	}

	public void setToolTip(String toolTip) {
		this.toolTip = toolTip;
	}

	@Override
	public int compareTo(Completion other) {
		return Integer.compare(relevance, other.getRelevance());
	}

	@Override
	public String toString() {
		return input;
	}
}
