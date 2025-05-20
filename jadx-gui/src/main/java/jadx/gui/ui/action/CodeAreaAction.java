package jadx.gui.ui.action;

import jadx.gui.ui.codearea.CodeArea;

public class CodeAreaAction extends JadxGuiAction {
	protected transient CodeArea codeArea;

	public CodeAreaAction(ActionModel actionModel, CodeArea codeArea) {
		super(actionModel);
		this.codeArea = codeArea;
		setShortcutComponent(codeArea);
	}

	public CodeAreaAction(String id, CodeArea codeArea) {
		super(id);
		this.codeArea = codeArea;
		setShortcutComponent(codeArea);
	}

	public void dispose() {
		codeArea = null;
	}
}
