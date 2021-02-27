package jadx.core.dex.attributes.nodes;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.ICodeNode;
import jadx.core.utils.ErrorsCounter;

public abstract class NotificationAttrNode extends LineAttrNode implements ICodeNode {
	private static final Logger LOG = LoggerFactory.getLogger(NotificationAttrNode.class);

	public void addError(String errStr, Throwable e) {
		ErrorsCounter.error(this, errStr, e);
	}

	public void addWarn(String warnStr) {
		ErrorsCounter.warning(this, warnStr);
	}

	public void addWarnComment(String warn) {
		addWarnComment(warn, null);
	}

	public void addWarnComment(String warn, @Nullable Throwable exc) {
		String commentStr = "JADX WARN: " + warn;
		addAttr(AType.COMMENTS, commentStr);
		if (exc != null) {
			LOG.warn("{} in {}", warn, this, exc);
		} else {
			LOG.warn("{} in {}", warn, this);
		}
	}

	public void addComment(String commentStr) {
		addAttr(AType.COMMENTS, commentStr);
	}

	public void addDebugComment(String commentStr) {
		addAttr(AType.COMMENTS, "JADX DEBUG: " + commentStr);
	}
}
