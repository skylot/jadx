package jadx.gui.ui.codearea.theme;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;

public class FallbackEditorTheme implements IEditorTheme {
	private Theme baseTheme;

	@Override
	public String getId() {
		return "fallback";
	}

	@Override
	public String getName() {
		return "Fallback";
	}

	@Override
	public void load() {
		baseTheme = new Theme(new RSyntaxTextArea());
	}

	@Override
	public void apply(RSyntaxTextArea textArea) {
		baseTheme.apply(textArea);
	}
}
