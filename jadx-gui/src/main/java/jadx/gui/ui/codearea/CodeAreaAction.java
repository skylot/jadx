package jadx.gui.ui.codearea;

import jadx.gui.ui.menu.ActionModel;
import jadx.gui.ui.menu.JadxGuiAction;

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
}
