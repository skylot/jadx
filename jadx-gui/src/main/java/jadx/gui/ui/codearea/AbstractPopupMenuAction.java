package jadx.gui.ui.codearea;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;

import org.fife.ui.rsyntaxtextarea.Token;

import jadx.api.JavaNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.utils.NLS;

public abstract class AbstractPopupMenuAction extends AbstractAction implements PopupMenuListener {
	private static final long serialVersionUID = -1860718486916678712L;
	protected final transient CodePanel contentPanel;
	protected final transient CodeArea codeArea;
	protected final transient JClass jCls;

	protected transient JavaNode node;

	protected AbstractPopupMenuAction(String name, CodePanel contentPanel, CodeArea codeArea, JClass jCls) {
		super(name);
		this.contentPanel = contentPanel;
		this.codeArea = codeArea;
		this.jCls = jCls;
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		node = null;
		Point pos = codeArea.getMousePosition();
		if (pos != null) {
			Token token = codeArea.viewToToken(pos);
			if (token != null) {
				node = codeArea.getJavaNodeAtOffset(jCls, token.getOffset());
			}
		}
		setEnabled(node != null);
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
