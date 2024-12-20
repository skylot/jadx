package jadx.gui.ui.codearea;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.event.PopupMenuEvent;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.api.data.ICodeComment;
import jadx.api.data.impl.JadxCodeComment;
import jadx.api.data.impl.JadxCodeData;
import jadx.api.data.impl.JadxCodeRef;
import jadx.api.data.impl.JadxNodeRef;
import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeAnnotation.AnnType;
import jadx.api.metadata.ICodeMetadata;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.metadata.annotations.InsnCodeOffset;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.gui.JadxWrapper;
import jadx.gui.settings.JadxProject;
import jadx.gui.treemodel.JClass;
import jadx.gui.ui.action.ActionModel;
import jadx.gui.ui.action.JadxGuiAction;
import jadx.gui.ui.dialog.CommentDialog;
import jadx.gui.utils.DefaultPopupMenuListener;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class CommentAction extends CodeAreaAction implements DefaultPopupMenuListener {
	private static final long serialVersionUID = 4753838562204629112L;

	private static final Logger LOG = LoggerFactory.getLogger(CommentAction.class);

	private final boolean enabled;
	private @Nullable ICodeComment actionComment;
	private boolean updateComment;

	public CommentAction(CodeArea codeArea) {
		super(ActionModel.CODE_COMMENT, codeArea);
		this.enabled = codeArea.getNode() instanceof JClass;
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		if (enabled && updateCommentAction(UiUtils.getOffsetAtMousePosition(codeArea))) {
			setNameAndDesc(updateComment ? NLS.str("popup.update_comment") : NLS.str("popup.add_comment"));
			setEnabled(true);
		} else {
			setEnabled(false);
		}
	}

	private boolean updateCommentAction(int pos) {
		ICodeComment codeComment = getCommentRef(pos);
		if (codeComment == null) {
			actionComment = null;
			return false;
		}
		ICodeComment exitsComment = searchForExistComment(codeComment);
		if (exitsComment != null) {
			actionComment = exitsComment;
			updateComment = true;
		} else {
			actionComment = codeComment;
			updateComment = false;
		}
		return true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (!enabled) {
			return;
		}
		if (JadxGuiAction.isSource(e)) {
			updateCommentAction(codeArea.getCaretPosition());
		}
		if (actionComment == null) {
			UiUtils.showMessageBox(codeArea.getMainWindow(), NLS.str("msg.cant_add_comment"));
			return;
		}
		CommentDialog.show(codeArea, actionComment, updateComment);
	}

	private @Nullable ICodeComment searchForExistComment(ICodeComment blankComment) {
		try {
			JadxProject project = codeArea.getProject();
			JadxCodeData codeData = project.getCodeData();
			if (codeData == null || codeData.getComments().isEmpty()) {
				return null;
			}
			for (ICodeComment comment : codeData.getComments()) {
				if (Objects.equals(comment.getNodeRef(), blankComment.getNodeRef())
						&& Objects.equals(comment.getCodeRef(), blankComment.getCodeRef())) {
					return comment;
				}
			}
		} catch (Exception e) {
			LOG.error("Error searching for exists comment", e);
		}
		return null;
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
			LOG.error("Failed to add comment at: {}", pos, e);
		}
		return null;
	}
}
