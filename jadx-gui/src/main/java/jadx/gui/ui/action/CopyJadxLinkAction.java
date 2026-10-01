package jadx.gui.ui.action;

import java.awt.event.ActionEvent;

import org.jetbrains.annotations.Nullable;

import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.utils.UiUtils;

/**
 * Copy jadx:// link to the class or method enclosing the click location,
 * see {@link jadx.gui.links.JadxLink}.
 * <p>
 * Unlike most code-area actions, this one resolves the <b>enclosing</b> node, so it is
 * available on any line inside a method (links to that method) or class (links to that class),
 * not only when clicking directly on a declaration name.
 */
public final class CopyJadxLinkAction extends JNodeAction {
	private static final long serialVersionUID = 3170418593527382140L;

	private transient @Nullable JNode enclosingNode;

	public CopyJadxLinkAction(CodeArea codeArea) {
		super(ActionModel.COPY_JADX_LINK, codeArea);
	}

	@Override
	public void changeNode(@Nullable JNode node) {
		// ignore the token-level node from the popup listener, use the enclosing method/class instead
		// (captured now, while the mouse is still over the click location)
		enclosingNode = getCodeArea().getEnclosingNodeUnderMouse();
		super.changeNode(enclosingNode);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JNode node = JadxGuiAction.isSource(e) ? getCodeArea().getEnclosingNodeUnderCaret() : enclosingNode;
		if (isActionEnabled(node)) {
			runAction(node);
		}
	}

	@Override
	public void runAction(JNode node) {
		String link = getCodeArea().getMainWindow().getLinkController().buildLink(node);
		if (link != null) {
			UiUtils.copyToClipboard(link);
		}
	}

	@Override
	public boolean isActionEnabled(@Nullable JNode node) {
		return node instanceof JMethod || node instanceof JClass;
	}

	@Override
	public void dispose() {
		super.dispose();
		enclosingNode = null;
	}
}
