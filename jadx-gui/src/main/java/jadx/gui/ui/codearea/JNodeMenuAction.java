package jadx.gui.ui.codearea;

import java.awt.*;
import java.awt.event.ActionEvent;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.fife.ui.rsyntaxtextarea.Token;
import org.jetbrains.annotations.Nullable;

import jadx.gui.utils.JumpPosition;

public abstract class JNodeMenuAction extends AbstractAction implements PopupMenuListener {

	protected final transient CodeArea codeArea;
	@Nullable
	protected transient JumpPosition jumpPos;

	public JNodeMenuAction(String name, CodeArea codeArea) {
		super(name);
		this.codeArea = codeArea;
	}

	@Override
	public abstract void actionPerformed(ActionEvent e);

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		jumpPos = getJumpPos();
		setEnabled(jumpPos != null);
	}

	@Nullable
	private JumpPosition getJumpPos() {
		Point pos = codeArea.getMousePosition();
		if (pos != null) {
			Token token = codeArea.viewToToken(pos);
			int offset = codeArea.adjustOffsetForToken(token);
			return codeArea.getDefPosForNodeAtOffset(offset);
		}
		return null;
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
