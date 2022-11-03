package jadx.gui.ui.treenodes;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import jadx.api.ICodeInfo;
import jadx.api.ResourceFile;
import jadx.api.impl.SimpleCodeInfo;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.ProcessState;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.Utils;
import jadx.gui.JadxWrapper;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.TabbedPane;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.panel.HtmlPanel;
import jadx.gui.utils.UiUtils;

public class SummaryNode extends JNode {
	private static final long serialVersionUID = 4295299814582784805L;

	private static final ImageIcon ICON = UiUtils.openSvgIcon("nodes/detailView");

	private final MainWindow mainWindow;
	private final JadxWrapper wrapper;

	public SummaryNode(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		this.wrapper = mainWindow.getWrapper();
	}

	@Override
	public ICodeInfo getCodeInfo() {
		StringEscapeUtils.Builder builder = StringEscapeUtils.builder(StringEscapeUtils.ESCAPE_HTML4);
		try {
			builder.append("<html>");
			builder.append("<body>");
			writeInputSummary(builder);
			writeDecompilationSummary(builder);
			builder.append("</body>");
		} catch (Exception e) {
			builder.append("Error build summary: ");
			builder.append("<pre>");
			builder.append(Utils.getStackTrace(e));
			builder.append("</pre>");
		}
		return new SimpleCodeInfo(builder.toString());
	}

	private void writeInputSummary(StringEscapeUtils.Builder builder) throws IOException {
		builder.append("<h2>Input</h2>");
		builder.append("<h3>Files</h3>");
		builder.append("<ul>");
		for (File inputFile : wrapper.getArgs().getInputFiles()) {
			builder.append("<li>");
			builder.escape(inputFile.getCanonicalFile().getAbsolutePath());
			builder.append("</li>");
		}
		builder.append("</ul>");

		List<ClassNode> classes = wrapper.getRootNode().getClasses(true);
		List<String> codeSources = classes.stream()
				.map(ClassNode::getInputFileName)
				.distinct()
				.sorted(Comparator.naturalOrder())
				.collect(Collectors.toList());
		codeSources.remove("synthetic");
		int codeSourcesCount = codeSources.size();
		builder.append("<h3>Code sources</h3>");
		builder.append("<ul>");
		if (codeSourcesCount != 1) {
			builder.append("<li>Count: " + codeSourcesCount + "</li>");
		}
		for (String input : codeSources) {
			builder.append("<li>");
			builder.escape(input);
			builder.append("</li>");
		}
		builder.append("</ul>");

		addNativeLibsInfo(builder);

		int methodsCount = classes.stream().mapToInt(cls -> cls.getMethods().size()).sum();
		int fieldsCount = classes.stream().mapToInt(cls -> cls.getFields().size()).sum();
		int insnCount = classes.stream().flatMap(cls -> cls.getMethods().stream()).mapToInt(MethodNode::getInsnsCount).sum();
		builder.append("<h3>Counts</h3>");
		builder.append("<ul>");
		builder.append("<li>Classes: " + classes.size() + "</li>");
		builder.append("<li>Methods: " + methodsCount + "</li>");
		builder.append("<li>Fields: " + fieldsCount + "</li>");
		builder.append("<li>Instructions: " + insnCount + " (units)</li>");
		builder.append("</ul>");
	}

	private void addNativeLibsInfo(StringEscapeUtils.Builder builder) {
		List<String> nativeLibs = wrapper.getResources().stream()
				.map(ResourceFile::getOriginalName)
				.filter(f -> f.endsWith(".so"))
				.sorted(Comparator.naturalOrder())
				.collect(Collectors.toList());
		builder.append("<h3>Native libs</h3>");
		builder.append("<ul>");
		if (nativeLibs.isEmpty()) {
			builder.append("<li>Total count: 0</li>");
		} else {
			Map<String, Set<String>> libsByArch = new HashMap<>();
			for (String libFile : nativeLibs) {
				String[] parts = StringUtils.split(libFile, '/');
				int count = parts.length;
				if (count >= 2) {
					String arch = parts[count - 2];
					String name = parts[count - 1];
					libsByArch.computeIfAbsent(arch, (a) -> new HashSet<>())
							.add(name);
				}
			}
			String arches = libsByArch.keySet().stream()
					.sorted(Comparator.naturalOrder())
					.collect(Collectors.joining(", "));
			builder.append("<li>Arch list: " + arches + "</li>");

			String perArchCount = libsByArch.entrySet().stream()
					.map(entry -> entry.getKey() + ":" + entry.getValue().size())
					.sorted(Comparator.naturalOrder())
					.collect(Collectors.joining(", "));
			builder.append("<li>Per arch count: " + perArchCount + "</li>");

			builder.append("<br>");
			builder.append("<li>Total count: " + nativeLibs.size() + "</li>");
			for (String lib : nativeLibs) {
				builder.append("<li>");
				builder.escape(lib);
				builder.append("</li>");
			}
		}
		builder.append("</ul>");
	}

	private void writeDecompilationSummary(StringEscapeUtils.Builder builder) {
		builder.append("<h2>Decompilation</h2>");
		List<ClassNode> classes = wrapper.getRootNode().getClassesWithoutInner();
		int classesCount = classes.size();
		long notLoadedClasses = classes.stream().filter(c -> c.getState() == ProcessState.NOT_LOADED).count();
		long loadedClasses = classes.stream().filter(c -> c.getState() == ProcessState.LOADED).count();
		long processedClasses = classes.stream().filter(c -> c.getState() == ProcessState.PROCESS_COMPLETE).count();
		long generatedClasses = classes.stream().filter(c -> c.getState() == ProcessState.GENERATED_AND_UNLOADED).count();
		builder.append("<ul>");
		builder.append("<li>Top level classes: " + classesCount + "</li>");
		builder.append("<li>Not loaded: " + valueAndPercent(notLoadedClasses, classesCount) + "</li>");
		builder.append("<li>Loaded: " + valueAndPercent(loadedClasses, classesCount) + "</li>");
		builder.append("<li>Processed: " + valueAndPercent(processedClasses, classesCount) + "</li>");
		builder.append("<li>Code generated: " + valueAndPercent(generatedClasses, classesCount) + "</li>");
		builder.append("</ul>");

		ErrorsCounter counter = wrapper.getRootNode().getErrorsCounter();
		Set<IAttributeNode> problemNodes = new HashSet<>();
		problemNodes.addAll(counter.getErrorNodes());
		problemNodes.addAll(counter.getWarnNodes());
		long problemMethods = problemNodes.stream().filter(MethodNode.class::isInstance).count();
		int methodsCount = classes.stream().mapToInt(cls -> cls.getMethods().size()).sum();
		double methodSuccessRate = (methodsCount - problemMethods) * 100.0 / (double) methodsCount;

		builder.append("<h3>Issues</h3>");
		builder.append("<ul>");
		builder.append("<li>Errors: " + counter.getErrorCount() + "</li>");
		builder.append("<li>Warnings: " + counter.getWarnsCount() + "</li>");
		builder.append("<li>Nodes with errors: " + counter.getErrorNodes().size() + "</li>");
		builder.append("<li>Nodes with warnings: " + counter.getWarnNodes().size() + "</li>");
		builder.append("<li>Total nodes with issues: " + problemNodes.size() + "</li>");
		builder.append("<li>Methods with issues: " + problemMethods + "</li>");
		builder.append("<li>Methods success rate: " + String.format("%.2f", methodSuccessRate) + "%</li>");
		builder.append("</ul>");
	}

	private String valueAndPercent(long value, int total) {
		return String.format("%d (%.2f%%)", value, value * 100 / ((double) total));
	}

	@Override
	public ContentPanel getContentPanel(TabbedPane tabbedPane) {
		return new HtmlPanel(tabbedPane, this);
	}

	@Override
	public String makeString() {
		return "Summary";
	}

	@Override
	public Icon getIcon() {
		return ICON;
	}

	@Override
	public JClass getJParent() {
		return null;
	}
}
