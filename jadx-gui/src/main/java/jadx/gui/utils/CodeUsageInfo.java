package jadx.gui.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.CodePosition;
import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.gui.treemodel.CodeNode;
import jadx.gui.treemodel.JNode;
import jadx.gui.utils.search.StringRef;

public class CodeUsageInfo {
	private static final Logger LOG = LoggerFactory.getLogger(CodeUsageInfo.class);

	public static class UsageInfo {
		private final List<CodeNode> usageList = new ArrayList<>();

		public List<CodeNode> getUsageList() {
			return usageList;
		}

		public synchronized void addUsage(CodeNode codeNode) {
			usageList.add(codeNode);
		}

		public synchronized void removeUsageIf(Predicate<? super CodeNode> filter) {
			usageList.removeIf(filter);
		}
	}

	private final JNodeCache nodeCache;

	public CodeUsageInfo(JNodeCache nodeCache) {
		this.nodeCache = nodeCache;
	}

	private final Map<JNode, UsageInfo> usageMap = new ConcurrentHashMap<>();

	public void processClass(JavaClass javaClass, CodeLinesInfo linesInfo, List<StringRef> lines) {
		try {
			Map<CodePosition, JavaNode> usage = javaClass.getUsageMap();
			for (Map.Entry<CodePosition, JavaNode> entry : usage.entrySet()) {
				CodePosition codePosition = entry.getKey();
				JavaNode javaNode = entry.getValue();
				addUsage(nodeCache.makeFrom(javaNode), javaClass, linesInfo, codePosition, lines);
			}
		} catch (Exception e) {
			LOG.error("Code usage process failed for class: {}", javaClass, e);
		}
	}

	private void addUsage(JNode jNode, JavaClass javaClass,
			CodeLinesInfo linesInfo, CodePosition codePosition, List<StringRef> lines) {
		UsageInfo usageInfo = usageMap.computeIfAbsent(jNode, key -> new UsageInfo());
		int line = codePosition.getLine();
		JavaNode javaNodeByLine = linesInfo.getJavaNodeByLine(line);
		StringRef codeLine = lines.get(line - 1);
		JNode node = nodeCache.makeFrom(javaNodeByLine == null ? javaClass : javaNodeByLine);
		CodeNode codeNode = new CodeNode(node, codeLine, line, codePosition.getPos());
		usageInfo.addUsage(codeNode);
	}

	public List<CodeNode> getUsageList(JNode node) {
		UsageInfo usageInfo = usageMap.get(node);
		if (usageInfo == null) {
			return Collections.emptyList();
		}
		return usageInfo.getUsageList();
	}

	public void remove(JavaClass cls) {
		usageMap.entrySet().removeIf(e -> {
			if (e.getKey().getJavaNode().getTopParentClass().equals(cls)) {
				return true;
			}
			e.getValue().removeUsageIf(node -> node.getJavaNode().getTopParentClass().equals(cls));
			return false;
		});
	}
}
