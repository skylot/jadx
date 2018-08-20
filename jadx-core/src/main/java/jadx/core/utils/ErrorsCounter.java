package jadx.core.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.attributes.nodes.JadxError;
import jadx.core.dex.attributes.nodes.JadxWarn;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.IDexNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.JadxOverflowException;

public class ErrorsCounter {
	private static final Logger LOG = LoggerFactory.getLogger(ErrorsCounter.class);

	private final Set<IAttributeNode> errorNodes = new HashSet<>();
	private int errorsCount;
	private final Set<IAttributeNode> warnNodes = new HashSet<>();
	private int warnsCount;

	public int getErrorCount() {
		return errorsCount;
	}

	public int getWarnsCount() {
		return warnsCount;
	}

	private synchronized <N extends IDexNode & IAttributeNode> String addError(N node, String error, @Nullable Throwable e) {
		errorNodes.add(node);
		errorsCount++;

		String msg = formatMsg(node, error);
		if (e == null) {
			LOG.error(msg);
		} else if (e instanceof JadxOverflowException) {
			// don't print full stack trace
			e = new JadxOverflowException(e.getMessage());
			LOG.error("{}, details: {}", msg, e.getMessage());
		} else {
			LOG.error(msg, e);
		}

		node.addAttr(AType.JADX_ERROR, new JadxError(error, e));
		node.remove(AFlag.INCONSISTENT_CODE);
		return msg;
	}

	private synchronized <N extends IDexNode & IAttributeNode> String addWarning(N node, String warn) {
		warnNodes.add(node);
		warnsCount++;

		node.addAttr(AType.JADX_WARN, new JadxWarn(warn));
		if (!node.contains(AType.JADX_ERROR)) {
			node.add(AFlag.INCONSISTENT_CODE);
		}

		String msg = formatMsg(node, warn);
		LOG.warn(msg);
		return msg;
	}

	public static String classError(ClassNode cls, String errorMsg, Throwable e) {
		return cls.dex().root().getErrorsCounter().addError(cls, errorMsg, e);
	}

	public static String classError(ClassNode cls, String errorMsg) {
		return classError(cls, errorMsg, null);
	}

	public static String methodError(MethodNode mth, String errorMsg, Throwable e) {
		return mth.root().getErrorsCounter().addError(mth, errorMsg, e);
	}

	public static String methodWarn(MethodNode mth, String warnMsg) {
		return mth.root().getErrorsCounter().addWarning(mth, warnMsg);
	}

	public static String formatMsg(IDexNode node, String msg) {
		return msg + " in " + node.typeName() + ": " + node + ", dex: " + node.dex().getDexFile().getName();
	}

	public void printReport() {
		if (getErrorCount() > 0) {
			LOG.error("{} errors occurred in following nodes:", getErrorCount());
			List<String> errors = new ArrayList<>(errorNodes.size());
			for (IAttributeNode node : errorNodes) {
				String nodeName = node.getClass().getSimpleName().replace("Node", "");
				errors.add(nodeName + ": " + node);
			}
			Collections.sort(errors);
			for (String err : errors) {
				LOG.error("  {}", err);
			}
		}
		if (getWarnsCount() > 0) {
			LOG.warn("{} warnings in {} nodes", getWarnsCount(), warnNodes.size());
		}
	}
}
