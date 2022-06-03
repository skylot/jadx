package jadx.gui.ui.codearea;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.jetbrains.annotations.Nullable;

import jadx.gui.treemodel.JNode;
import jadx.gui.utils.UiUtils;

/**
 * Add menu and key binding actions for JNode in code area
 */
public abstract class JNodeAction extends AbstractAction {
	private static final long serialVersionUID = -2600154727884853550L;

	private transient CodeArea codeArea;
	private transient @Nullable JNode node;

	public JNodeAction(String name, CodeArea codeArea) {
		super(name);
		this.codeArea = codeArea;
	}

	public abstract void runAction(JNode node);

	public boolean isActionEnabled(@Nullable JNode node) {
		return node != null;
	}

	public void addKeyBinding(KeyStroke key, String id) {
		UiUtils.addKeyBinding(codeArea, key, id, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				node = codeArea.getNodeUnderCaret();
				if (isActionEnabled(node)) {
					runAction(node);
				}
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		runAction(node);
	}

	public void changeNode(@Nullable JNode node) {
		this.node = node;
		setEnabled(isActionEnabled(node));
	}

	public CodeArea getCodeArea() {
		return codeArea;
	}

	public void dispose() {
		node = null;
		codeArea = null;
		for (PropertyChangeListener changeListener : getPropertyChangeListeners()) {
			removePropertyChangeListener(changeListener);
		}
	}
}
