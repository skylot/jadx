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
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.attributes.nodes.JadxErrorAttr;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.JadxOverflowException;

public class ErrorsCounter {
	private static final Logger LOG = LoggerFactory.getLogger(ErrorsCounter.class);

	private final Set<IAttributeNode> errorNodes = new HashSet<>();
	private int errorsCount;

	public int getErrorCount() {
		return errorsCount;
	}

	private synchronized void addError(IAttributeNode node, String msg, @Nullable Throwable e) {
		errorNodes.add(node);
		errorsCount++;

		if (e != null) {
			if (e instanceof JadxOverflowException) {
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
	}

	public static String formatErrorMsg(ClassNode cls, String msg) {
		return msg + " in class: " + cls + ", dex: " + cls.dex().getDexFile().getName();
	}

	public static String formatErrorMsg(MethodNode mth, String msg) {
		return msg + " in method: " + mth + ", dex: " + mth.dex().getDexFile().getName();
	}
}
