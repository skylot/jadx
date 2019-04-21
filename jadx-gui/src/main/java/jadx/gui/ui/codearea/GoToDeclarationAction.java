package jadx.gui.ui.codearea;

import java.awt.Point;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.fife.ui.rsyntaxtextarea.Token;

import jadx.api.JavaNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.NLS;

public final class GoToDeclarationAction extends AbstractAction implements PopupMenuListener {
	private static final long serialVersionUID = -1186470538894941301L;
	private final transient CodePanel contentPanel;
	private final transient CodeArea codeArea;
	private final transient JClass jCls;

	private transient JavaNode node;

	public GoToDeclarationAction(CodePanel contentPanel, CodeArea codeArea, JClass jCls) {
		super(NLS.str("popup.go_to_declaration"));
		this.contentPanel = contentPanel;
		this.codeArea = codeArea;
		this.jCls = jCls;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (node == null) {
			return;
		}
		MainWindow mainWindow = contentPanel.getTabbedPane().getMainWindow();
		JNode jNode = mainWindow.getCacheObject().getNodeCache().makeFrom(node);
		mainWindow.getTabbedPane().codeJump(new JumpPosition(jNode, jNode.getLine()));
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
