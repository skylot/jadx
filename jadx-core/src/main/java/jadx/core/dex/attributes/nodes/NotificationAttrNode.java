package jadx.core.dex.attributes.nodes;

import jadx.api.CommentsLevel;
import jadx.api.ICodeWriter;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.ICodeNode;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.Utils;

public abstract class NotificationAttrNode extends LineAttrNode implements ICodeNode {

	public boolean checkCommentsLevel(CommentsLevel required) {
		return required.filter(this.root().getArgs().getCommentsLevel());
	}

	public void addError(String errStr, Throwable e) {
		ErrorsCounter.error(this, errStr, e);
	}

	public void addWarn(String warn) {
		ErrorsCounter.warning(this, warn);
		initCommentsAttr().add(CommentsLevel.WARN, warn);
		this.add(AFlag.INCONSISTENT_CODE);
	}

	public void addCodeComment(String comment) {
		addAttr(AType.CODE_COMMENTS, comment);
	}

	public void addWarnComment(String warn) {
		initCommentsAttr().add(CommentsLevel.WARN, warn);
	}

	public void addWarnComment(String warn, Throwable exc) {
		String commentStr = warn + ICodeWriter.NL + Utils.getStackTrace(exc);
		initCommentsAttr().add(CommentsLevel.WARN, commentStr);
	}

	public void addInfoComment(String commentStr) {
		initCommentsAttr().add(CommentsLevel.INFO, commentStr);
	}

	public void addDebugComment(String commentStr) {
		initCommentsAttr().add(CommentsLevel.DEBUG, commentStr);
	}

	public CommentsLevel getCommentsLevel() {
		return this.root().getArgs().getCommentsLevel();
	}

	private JadxCommentsAttr initCommentsAttr() {
		JadxCommentsAttr commentsAttr = this.get(AType.JADX_COMMENTS);
		if (commentsAttr == null) {
			commentsAttr = new JadxCommentsAttr();
			this.addAttr(commentsAttr);
		}
		return commentsAttr;
	}
}
