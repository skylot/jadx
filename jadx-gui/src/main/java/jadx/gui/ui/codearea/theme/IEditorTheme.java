package jadx.gui.ui.codearea.theme;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

public interface IEditorTheme {

	String getId();

	String getName();

	default void load() {
		// optional method
	}

	void apply(RSyntaxTextArea textArea);

	default void unload() {
		// optional method
	}
}
