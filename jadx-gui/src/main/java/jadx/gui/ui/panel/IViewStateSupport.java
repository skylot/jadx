package jadx.gui.ui.panel;

import jadx.gui.ui.codearea.EditorViewState;

public interface IViewStateSupport {

	EditorViewState getEditorViewState();

	void restoreEditorViewState(EditorViewState viewState);
}
