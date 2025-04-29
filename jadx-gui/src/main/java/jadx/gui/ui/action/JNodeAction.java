package jadx.gui.ui.action;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;

import org.jetbrains.annotations.Nullable;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.codearea.CodeArea;

/**
 * Add menu and key binding actions for JNode in code area
 */
public abstract class JNodeAction extends CodeAreaAction {
	private static final long serialVersionUID = -2600154727884853550L;

	private transient @Nullable JNode node;

	public JNodeAction(ActionModel actionModel, CodeArea codeArea) {
		super(actionModel, codeArea);
	}

	public JNodeAction(String id, CodeArea codeArea) {
		super(id, codeArea);
	}

	public abstract void runAction(JNode node);

	public boolean isActionEnabled(@Nullable JNode node) {
		return node != null;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (JadxGuiAction.isSource(e)) {
			node = codeArea.getNodeUnderCaret();
			if (isActionEnabled(node)) {
				runAction(node);
			}
		} else {
			runAction(node);
		}
	}

	public void changeNode(@Nullable JNode node) {
		this.node = node;
		setEnabled(isActionEnabled(node));
	}

	public CodeArea getCodeArea() {
		return codeArea;
	}

	@Override
	public void dispose() {
		super.dispose();
		node = null;
		for (PropertyChangeListener changeListener : getPropertyChangeListeners()) {
			removePropertyChangeListener(changeListener);
		}
	}
}
