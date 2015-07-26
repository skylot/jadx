package jadx.gui.utils;

import jadx.api.CodePosition;
import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.gui.treemodel.CodeNode;
import jadx.gui.treemodel.JNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeUsageInfo {

	public static class UsageInfo {
		private final List<CodeNode> usageList = new ArrayList<CodeNode>();

		public List<CodeNode> getUsageList() {
			return usageList;
		}
	}

	private final Map<JNode, UsageInfo> usageMap = new HashMap<JNode, UsageInfo>();

	public void processClass(JavaClass javaClass, CodeLinesInfo linesInfo, String[] lines) {
		Map<CodePosition, JavaNode> usage = javaClass.getUsageMap();
		for (Map.Entry<CodePosition, JavaNode> entry : usage.entrySet()) {
			CodePosition codePosition = entry.getKey();
			JavaNode javaNode = entry.getValue();
			addUsage(JNode.makeFrom(javaNode), javaClass, linesInfo, codePosition, lines);
		}
	}

	private void addUsage(JNode jNode, JavaClass javaClass,
			CodeLinesInfo linesInfo, CodePosition codePosition, String[] lines) {
		UsageInfo usageInfo = usageMap.get(jNode);
		if (usageInfo == null) {
			usageInfo = new UsageInfo();
			usageMap.put(jNode, usageInfo);
		}
		int line = codePosition.getLine();
		JavaNode javaNodeByLine = linesInfo.getJavaNodeByLine(line);
		String codeLine = lines[line - 1].trim();
		CodeNode codeNode = new CodeNode(javaNodeByLine == null ? javaClass : javaNodeByLine, line, codeLine);
		usageInfo.getUsageList().add(codeNode);
	}

	public List<CodeNode> getUsageList(JNode node) {
		UsageInfo usageInfo = usageMap.get(node);
		if (usageInfo == null) {
			return Collections.emptyList();
		}
		return usageInfo.getUsageList();
	}
}
