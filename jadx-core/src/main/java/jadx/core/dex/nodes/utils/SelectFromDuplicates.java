package jadx.core.dex.nodes.utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.nodes.ClassNode;

/**
 * Select best class from list of classes with same full name
 * Current implementation: use class with source file as 'classesN.dex' where N is minimal
 */
public class SelectFromDuplicates {
	private static final Logger LOG = LoggerFactory.getLogger(SelectFromDuplicates.class);

	private static final Pattern CLASSES_DEX_PATTERN = Pattern.compile("classes([1-9]\\d*)\\.dex");

	public static ClassNode process(List<ClassNode> dupClsList) {
		ClassNode bestCls = null;
		int bestClsIndex = -1;
		for (ClassNode clsNode : dupClsList) {
			boolean selectCurrent = false;
			if (bestCls == null) {
				selectCurrent = true;
			} else {
				int clsIndex = getClassesIndex(clsNode.getInputFileName());
				if (clsIndex != -1) {
					if (bestClsIndex != -1) {
						// if both are valid, the lower index has precedence
						if (clsIndex < bestClsIndex) {
							selectCurrent = true;
						}
					} else {
						// valid dex names have precedence
						selectCurrent = true;
					}
				}
			}
			if (selectCurrent) {
				bestCls = clsNode;
				bestClsIndex = getClassesIndex(clsNode.getInputFileName());
			}
		}
		return bestCls;
	}

	/**
	 * Get N from classesN.dex
	 *
	 * @return -1 if source is not valid dex name
	 */
	private static int getClassesIndex(String source) {
		if ("classes.dex".equals(source)) {
			return 1;
		}
		try {
			Matcher matcher = CLASSES_DEX_PATTERN.matcher(source);
			if (!matcher.matches()) {
				return -1;
			}
			String num = matcher.group(1);
			if (num.equals("1")) {
				return -1;
			}
			return Integer.parseInt(num);
		} catch (Exception e) {
			LOG.debug("Failed to parse source classes index", e);
			return -1;
		}
	}
}
