package jadx.gui.ui.codearea;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.CodePosition;
import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.api.data.ICodeComment;
import jadx.api.data.annotations.CustomOffsetRef;
import jadx.api.data.annotations.InsnCodeOffset;
import jadx.api.data.impl.JadxCodeComment;
import jadx.api.data.impl.JadxNodeRef;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.CommentDialog;
import jadx.gui.utils.CodeLinesInfo;
import jadx.gui.utils.DefaultPopupMenuListener;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

import static javax.swing.KeyStroke.getKeyStroke;

public class CommentAction extends AbstractAction implements DefaultPopupMenuListener {
	private static final long serialVersionUID = 4753838562204629112L;

	private static final Logger LOG = LoggerFactory.getLogger(CommentAction.class);
	private final CodeArea codeArea;
	private final JavaClass topCls;

	private ICodeComment actionComment;

	public CommentAction(CodeArea codeArea) {
		super(NLS.str("popup.add_comment") + " (;)");
		this.codeArea = codeArea;
		JNode topNode = codeArea.getNode();
		if (topNode instanceof JClass) {
			this.topCls = ((JClass) topNode).getCls();
		} else {
			this.topCls = null;
		}

		KeyStroke key = getKeyStroke(KeyEvent.VK_SEMICOLON, 0);
		codeArea.getInputMap().put(key, "popup.add_comment");
		codeArea.getActionMap().put("popup.add_comment", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int line = codeArea.getCaretLineNumber() + 1;
				ICodeComment codeComment = getCommentRef(line);
				showCommentDialog(codeComment);
			}
		});
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		ICodeComment codeComment = getCommentRef(getMouseLine());
		setEnabled(codeComment != null);
		this.actionComment = codeComment;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		showCommentDialog(this.actionComment);
	}

	private void showCommentDialog(ICodeComment codeComment) {
		if (codeComment == null) {
			UiUtils.showMessageBox(codeArea.getMainWindow(), NLS.str("msg.cant_add_comment"));
			return;
		}
		CommentDialog.show(codeArea, codeComment);
	}

	/**
	 * Check if possible insert comment at current line.
	 *
	 * @return blank code comment object (comment string empty)
	 */
	@Nullable
	private ICodeComment getCommentRef(int line) {
		if (line == -1 || this.topCls == null) {
			return null;
		}
		try {
			CodeLinesInfo linesInfo = new CodeLinesInfo(topCls, true); // TODO: cache and update on class refresh
			// add comment if node definition at this line
			JavaNode nodeAtLine = linesInfo.getDefAtLine(line);
			if (nodeAtLine != null) {
				// at node definition -> add comment for it
				JadxNodeRef nodeRef = JadxNodeRef.forJavaNode(nodeAtLine);
				return new JadxCodeComment(nodeRef, "");
			}
			Object ann = topCls.getAnnotationAt(new CodePosition(line));
			if (ann == null) {
				// check if line with comment above node definition
				try {
					JavaNode defNode = linesInfo.getJavaNodeBelowLine(line);
					if (defNode != null) {
						String lineStr = codeArea.getLineText(line).trim();
						if (lineStr.startsWith("//")) {
							return new JadxCodeComment(JadxNodeRef.forJavaNode(defNode), "");
						}
					}
				} catch (Exception e) {
					LOG.error("Failed to check comment line: " + line, e);
				}
				return null;
			}

			// try to add method line comment
			JavaNode node = linesInfo.getJavaNodeByLine(line);
			if (node instanceof JavaMethod) {
				JadxNodeRef nodeRef = JadxNodeRef.forMth((JavaMethod) node);
				if (ann instanceof InsnCodeOffset) {
					int rawOffset = ((InsnCodeOffset) ann).getOffset();
					return new JadxCodeComment(nodeRef, "", rawOffset);
				}
				if (ann instanceof CustomOffsetRef) {
					CustomOffsetRef customRef = (CustomOffsetRef) ann;
					JadxCodeComment comment = new JadxCodeComment(nodeRef, "", customRef.getOffset());
					comment.setAttachType(customRef.getAttachType());
					return comment;
				}
			}
		} catch (Exception e) {
			LOG.error("Failed to add comment at line: " + line, e);
		}
		return null;
	}

	private int getMouseLine() {
		int closestOffset = UiUtils.getOffsetAtMousePosition(codeArea);
		if (closestOffset == -1) {
			return -1;
		}
		try {
			return codeArea.getLineOfOffset(closestOffset) + 1;
		} catch (Exception e) {
			LOG.debug("Failed to get line by offset: {}", closestOffset);
			return -1;
		}
	}
}
