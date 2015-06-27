package jadx.core.utils;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.attributes.nodes.JadxErrorAttr;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.JadxOverflowException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorsCounter {
	private static final Logger LOG = LoggerFactory.getLogger(ErrorsCounter.class);

	private final Set<Object> errorNodes = new HashSet<Object>();
	private int errorsCount;

	public int getErrorCount() {
		return errorsCount;
	}

	public void reset() {
		errorNodes.clear();
		errorsCount = 0;
	}

	private void addError(IAttributeNode node, String msg, Throwable e) {
		errorNodes.add(node);
		errorsCount++;

		if (e != null) {
			if (e.getClass() == JadxOverflowException.class) {
				// don't print full stack trace
				e = new JadxOverflowException(e.getMessage());
				LOG.error("{}, message: {}", msg, e.getMessage());
			} else {
				LOG.error(msg, e);
			}
			node.addAttr(new JadxErrorAttr(e));
		} else {
			node.add(AFlag.INCONSISTENT_CODE);
			LOG.error(msg);
		}
	}

	public static String classError(ClassNode cls, String errorMsg, Throwable e) {
		String msg = formatErrorMsg(cls, errorMsg);
		cls.dex().root().getErrorsCounter().addError(cls, msg, e);
		return msg;
	}

	public static String classError(ClassNode cls, String errorMsg) {
		return classError(cls, errorMsg, null);
	}

	public static String methodError(MethodNode mth, String errorMsg, Throwable e) {
		String msg = formatErrorMsg(mth, errorMsg);
		mth.dex().root().getErrorsCounter().addError(mth, msg, e);
		return msg;
	}

	public static String methodError(MethodNode mth, String errorMsg) {
		return methodError(mth, errorMsg, null);
	}

	public void printReport() {
		if (getErrorCount() > 0) {
			LOG.error("{} errors occurred in following nodes:", getErrorCount());
			List<Object> nodes = new ArrayList<Object>(errorNodes);
			Collections.sort(nodes, new Comparator<Object>() {
				@Override
				public int compare(Object o1, Object o2) {
					return String.valueOf(o1).compareTo(String.valueOf(o2));
				}
			});
			for (Object node : nodes) {
				String nodeName = node.getClass().getSimpleName().replace("Node", "");
				LOG.error("  {}: {}", nodeName, node);
			}
		}
	}

	public static String formatErrorMsg(ClassNode cls, String msg) {
		return msg + " in class: " + cls;
	}

	public static String formatErrorMsg(MethodNode mth, String msg) {
		return msg + " in method: " + mth;
	}

	private String formatException(Throwable e) {
		if (e == null || e.getMessage() == null) {
			return "";
		} else {
			return "\n error: " + e.getMessage();
		}
	}

	public String formatErrorMsg(ClassNode cls, String msg, Throwable e) {
		return formatErrorMsg(cls, msg) + formatException(e);
	}

	public String formatErrorMsg(MethodNode mth, String msg, Throwable e) {
		return formatErrorMsg(mth, msg) + formatException(e);
	}
}
