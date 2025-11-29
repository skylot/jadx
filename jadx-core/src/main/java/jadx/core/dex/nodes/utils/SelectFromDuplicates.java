package jadx.core.dex.nodes.utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.nodes.ClassNode;

public class SelectFromDuplicates {
	private static final Logger LOG = LoggerFactory.getLogger(SelectFromDuplicates.class);

	private static final Pattern CLASSES_DEX_PATTERN = Pattern.compile("classes(\\d*)\\.dex");

	public static ClassNode process(List<ClassNode> dupClsList) {
		ClassNode bestCls = null;
		for (ClassNode clsNode : dupClsList) {
			if (bestCls == null) {
				bestCls = clsNode;
			} else {
				String bestFileName = bestCls.getInputFileName();
				String fileName = clsNode.getInputFileName();
				if (isClassesDex(fileName)) {
					if (isClassesDex(bestFileName)) {
						// if both are valid, the lower index has precedence
						if (getClassesIndex(fileName) < getClassesIndex(bestFileName)) {
							bestCls = clsNode;
						}
					} else {
						// valid dex names have precedence
						bestCls = clsNode;
					}
				}
			}
		}
		return bestCls;
	}

	private static boolean isClassesDex(String source) {
		return source != null
				&& !source.isEmpty()
				&& CLASSES_DEX_PATTERN.matcher(source).matches();
	}

	private static int getClassesIndex(String source) {
		try {
			Matcher matcher = CLASSES_DEX_PATTERN.matcher(source);
			if (!matcher.matches()) {
				return Integer.MAX_VALUE;
			}
			String num = matcher.group(1);
			if (num.isEmpty()) {
				return 0;
			}
			return Integer.parseInt(num);
		} catch (Exception e) {
			LOG.debug("Failed to parse source classes index", e);
			return Integer.MAX_VALUE;
		}
	}
}
