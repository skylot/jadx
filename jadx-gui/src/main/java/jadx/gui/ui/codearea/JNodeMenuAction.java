package jadx.gui.ui.codearea;

import java.awt.Point;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.fife.ui.rsyntaxtextarea.Token;
import org.jetbrains.annotations.Nullable;

import jadx.gui.utils.UiUtils;

public abstract class JNodeMenuAction<T> extends AbstractAction implements PopupMenuListener {
	private static final long serialVersionUID = -2600154727884853550L;

	protected final transient CodeArea codeArea;
	@Nullable
	protected transient T node;

	public JNodeMenuAction(String name, CodeArea codeArea) {
		super(name);
		this.codeArea = codeArea;
	}

	@Override
	public abstract void actionPerformed(ActionEvent e);

	@Nullable
	public abstract T getNodeByOffset(int offset);

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		node = getNode();
		setEnabled(node != null);
	}

	@Nullable
	private T getNode() {
		Point pos = UiUtils.getMousePosition(codeArea);
		Token token = codeArea.viewToToken(pos);
		int offset = codeArea.adjustOffsetForToken(token);
		return getNodeByOffset(offset);
	}

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		// do nothing
	}

	@Override
	public void popupMenuCanceled(PopupMenuEvent e) {
		// do nothing
	}
}
