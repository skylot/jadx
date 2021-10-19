package jadx.core.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.attributes.nodes.JadxError;
import jadx.core.dex.nodes.IDexNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.JadxOverflowException;

public class ErrorsCounter {
	private static final Logger LOG = LoggerFactory.getLogger(ErrorsCounter.class);
	private static final boolean PRINT_MTH_SIZE = true;

	private final Set<IAttributeNode> errorNodes = new HashSet<>();
	private int errorsCount;
	private final Set<IAttributeNode> warnNodes = new HashSet<>();
	private int warnsCount;

	public static <N extends IDexNode & IAttributeNode> String error(N node, String warnMsg, Throwable th) {
		return node.root().getErrorsCounter().addError(node, warnMsg, th);
	}

	public static <N extends IDexNode & IAttributeNode> void warning(N node, String warnMsg) {
		node.root().getErrorsCounter().addWarning(node, warnMsg);
	}

	public static String formatMsg(IDexNode node, String msg) {
		return msg + " in " + node.typeName() + ": " + node + ", file: " + node.getInputFileName();
	}

	private synchronized <N extends IDexNode & IAttributeNode> String addError(N node, String error, @Nullable Throwable e) {
		errorNodes.add(node);
		errorsCount++;

		String msg = formatMsg(node, error);
		if (PRINT_MTH_SIZE && node instanceof MethodNode) {
			msg = "[" + ((MethodNode) node).getInsnsCount() + "] " + msg;
		}
		if (e == null) {
			LOG.error(msg);
		} else if (e instanceof StackOverflowError) {
			LOG.error("{}, error: StackOverflowError", msg);
		} else if (e instanceof JadxOverflowException) {
			// don't print full stack trace
			String details = e.getMessage();
			e = new JadxOverflowException(details);
			if (details == null || details.isEmpty()) {
				LOG.error("{}", msg);
			} else {
				LOG.error("{}, details: {}", msg, details);
			}
		} else {
			LOG.error(msg, e);
		}
		node.addAttr(AType.JADX_ERROR, new JadxError(error, e));
		return msg;
	}

	private synchronized <N extends IDexNode & IAttributeNode> void addWarning(N node, String warn) {
		warnNodes.add(node);
		warnsCount++;
		LOG.warn(formatMsg(node, warn));
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

	public int getErrorCount() {
		return errorsCount;
	}

	public int getWarnsCount() {
		return warnsCount;
	}

	public Set<IAttributeNode> getErrorNodes() {
		return errorNodes;
	}

	public Set<IAttributeNode> getWarnNodes() {
		return warnNodes;
	}
}
