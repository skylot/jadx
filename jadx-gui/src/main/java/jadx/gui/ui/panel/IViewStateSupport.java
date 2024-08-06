package jadx.gui.ui.panel;

import jadx.gui.ui.codearea.EditorViewState;

public interface IViewStateSupport {

	void saveEditorViewState(EditorViewState viewState);

	void restoreEditorViewState(EditorViewState viewState);
}
