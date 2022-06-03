package jadx.gui.ui.codearea;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.event.PopupMenuEvent;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.api.data.ICodeComment;
import jadx.api.data.impl.JadxCodeComment;
import jadx.api.data.impl.JadxCodeRef;
import jadx.api.data.impl.JadxNodeRef;
import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeAnnotation.AnnType;
import jadx.api.metadata.ICodeMetadata;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.metadata.annotations.InsnCodeOffset;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.gui.JadxWrapper;
import jadx.gui.treemodel.JClass;
import jadx.gui.ui.dialog.CommentDialog;
import jadx.gui.utils.DefaultPopupMenuListener;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

import static javax.swing.KeyStroke.getKeyStroke;

public class CommentAction extends AbstractAction implements DefaultPopupMenuListener {
	private static final long serialVersionUID = 4753838562204629112L;

	private static final Logger LOG = LoggerFactory.getLogger(CommentAction.class);
	private final CodeArea codeArea;
	private final boolean enabled;

	private ICodeComment actionComment;

	public CommentAction(CodeArea codeArea) {
		super(NLS.str("popup.add_comment") + " (;)");
		this.codeArea = codeArea;
		this.enabled = codeArea.getNode() instanceof JClass;
		if (enabled) {
			UiUtils.addKeyBinding(codeArea, getKeyStroke(KeyEvent.VK_SEMICOLON, 0), "popup.add_comment",
					() -> showCommentDialog(getCommentRef(codeArea.getCaretPosition())));
		}
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		if (enabled) {
			ICodeComment codeComment = getCommentRef(UiUtils.getOffsetAtMousePosition(codeArea));
			setEnabled(codeComment != null);
			this.actionComment = codeComment;
		} else {
			setEnabled(false);
		}
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
	private ICodeComment getCommentRef(int pos) {
		if (pos == -1) {
			return null;
		}
		try {
			JadxWrapper wrapper = codeArea.getJadxWrapper();
			ICodeInfo codeInfo = codeArea.getCodeInfo();
			ICodeMetadata metadata = codeInfo.getCodeMetadata();
			int lineStartPos = codeArea.getLineStartFor(pos);

			// add method line comment by instruction offset
			ICodeAnnotation offsetAnn = metadata.searchUp(pos, lineStartPos, AnnType.OFFSET);
			if (offsetAnn instanceof InsnCodeOffset) {
				JavaNode node = wrapper.getJavaNodeByRef(metadata.getNodeAt(pos));
				if (node instanceof JavaMethod) {
					int rawOffset = ((InsnCodeOffset) offsetAnn).getOffset();
					JadxNodeRef nodeRef = JadxNodeRef.forMth((JavaMethod) node);
					return new JadxCodeComment(nodeRef, JadxCodeRef.forInsn(rawOffset), "");
				}
			}

			// check for definition at this line
			ICodeNodeRef nodeDef = metadata.searchUp(pos, (off, ann) -> {
				if (lineStartPos <= off && ann.getAnnType() == AnnType.DECLARATION) {
					ICodeNodeRef defRef = ((NodeDeclareRef) ann).getNode();
					if (defRef.getAnnType() != AnnType.VAR) {
						return defRef;
					}
				}
				return null;
			});
			if (nodeDef != null) {
				JadxNodeRef nodeRef = JadxNodeRef.forJavaNode(wrapper.getJavaNodeByRef(nodeDef));
				return new JadxCodeComment(nodeRef, "");
			}

			// check if at comment above node definition
			String lineStr = codeArea.getLineAt(pos).trim();
			if (lineStr.startsWith("//") || lineStr.startsWith("/*")) {
				ICodeNodeRef nodeRef = metadata.searchDown(pos, (off, ann) -> {
					if (off > pos && ann.getAnnType() == AnnType.DECLARATION) {
						return ((NodeDeclareRef) ann).getNode();
					}
					return null;
				});
				if (nodeRef != null) {
					JavaNode defNode = wrapper.getJavaNodeByRef(nodeRef);
					return new JadxCodeComment(JadxNodeRef.forJavaNode(defNode), "");
				}
			}
		} catch (Exception e) {
			LOG.error("Failed to add comment at: " + pos, e);
		}
		return null;
	}
}
