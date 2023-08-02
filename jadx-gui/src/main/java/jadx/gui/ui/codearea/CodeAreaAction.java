package jadx.gui.ui.codearea;

import jadx.gui.ui.action.ActionModel;
import jadx.gui.ui.action.JadxGuiAction;

public class CodeAreaAction extends JadxGuiAction {
	protected transient CodeArea codeArea;

	public CodeAreaAction(ActionModel actionModel, CodeArea codeArea) {
		super(actionModel);
		this.codeArea = codeArea;
		setTargetComponent(codeArea);
	}

	public CodeAreaAction(String id, CodeArea codeArea) {
		super(id);
		this.codeArea = codeArea;
		setTargetComponent(codeArea);
	}

	public void dispose() {
		codeArea = null;
	}
}
