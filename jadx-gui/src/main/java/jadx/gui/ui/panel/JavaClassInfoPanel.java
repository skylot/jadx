package jadx.gui.ui.panel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.HyperlinkEvent;

import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeInfo;
import jadx.api.ResourceFile;
import jadx.api.ResourcesLoader;
import jadx.api.impl.SimpleCodeInfo;
import jadx.api.plugins.input.data.AccessFlags;
import jadx.api.plugins.input.data.AccessFlagsScope;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.xmlgen.ResContainer;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.codearea.BinaryContentPanel;
import jadx.gui.ui.codearea.CodeContentPanel;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.utils.NLS;
import jadx.plugins.input.java.JavaClassReader;
import jadx.plugins.input.java.data.JavaClassData;

public class JavaClassInfoPanel extends ContentPanel {
	private static final long serialVersionUID = -3184567238945102938L;

	private final List<JClass> links = new ArrayList<>();
	private final HtmlPanel htmlPanel;
	private final @Nullable JavaClassData clsData;

	public static void buildTabs(JTabbedPane resourceTabbedPane, TabbedPane panel, JResource resource) {
		JavaClassInfoPanel infoPanel = new JavaClassInfoPanel(panel, resource);
		resourceTabbedPane.addTab(NLS.str("tabs.info"), infoPanel);
		resourceTabbedPane.addTab(NLS.str("tabs.smali"), infoPanel.createDisassemblePanel());
		resourceTabbedPane.addTab(NLS.str("tabs.hex"), new BinaryContentPanel(panel, resource));
	}

	public JavaClassInfoPanel(TabbedPane panel, JResource resource) {
		super(panel, resource);
		setLayout(new BorderLayout());

		JavaClassData loadedData = null;
		JClass resolvedCls = null;
		String html;
		try {
			byte[] bytes = loadBytes(resource);
			loadedData = new JavaClassData(new JavaClassReader(0, resource.getName(), bytes));
			String rawName = Utils.cleanObjectName(loadedData.getType());
			resolvedCls = resolveJClass(panel.getMainWindow(), rawName);
			html = buildInfoHtml(panel.getMainWindow(), resource, loadedData, rawName, resolvedCls, bytes.length);
		} catch (Exception e) {
			html = buildErrorHtml(e);
		}
		this.clsData = loadedData;

		if (resolvedCls != null) {
			JClass targetCls = resolvedCls;
			JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
			JButton openBtn = new JButton("Open decompiled code", targetCls.getIcon());
			openBtn.addActionListener(ev -> panel.getTabsController().codeJump(targetCls));
			topPanel.add(openBtn);
			add(topPanel, BorderLayout.NORTH);
		}

		InfoHtmlNode htmlNode = new InfoHtmlNode(resource.getName(), html);
		this.htmlPanel = new HtmlPanel(panel, htmlNode);
		this.htmlPanel.getHtmlArea().addHyperlinkListener(event -> {
			if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				int idx = Integer.parseInt(event.getDescription());
				if (idx >= 0 && idx < links.size()) {
					panel.getTabsController().codeJump(links.get(idx));
				}
			}
		});
		add(this.htmlPanel, BorderLayout.CENTER);
	}

	public ContentPanel createDisassemblePanel() {
		return new CodeContentPanel(tabbedPane, new DisasmNode(node.getName(), clsData), false);
	}

	private String buildInfoHtml(MainWindow mainWindow, JResource resource, JavaClassData data,
			String rawName, @Nullable JClass resolvedCls, int fileSize) {
		StringEscapeUtils.Builder builder = StringEscapeUtils.builder(StringEscapeUtils.ESCAPE_HTML4);
		builder.append("<html><body>");
		builder.append("<h2>Java class details</h2>");
		builder.append("<ul>");

		addLine(builder, "Resource path", resource.getName());
		addLine(builder, "Class raw name", rawName);
		if (resolvedCls != null) {
			builder.append("<li><b>Decompiled class:</b> ");
			appendClassLink(builder, resolvedCls, resolvedCls.getFullName());
			builder.append("</li>");
		}
		addLine(builder, "Bytecode version", data.getJavaVersion());

		int accessFlags = data.getAccessFlags();
		String formattedFlags = AccessFlags.format(accessFlags, AccessFlagsScope.CLASS).trim();
		String flagsStr = formattedFlags.isEmpty()
				? String.format("0x%04x", accessFlags)
				: String.format("%s (0x%04x)", formattedFlags, accessFlags);
		addLine(builder, "Access flags", flagsStr);

		String superType = data.getSuperType();
		if (superType != null) {
			String superName = Utils.cleanObjectName(superType);
			builder.append("<li><b>Super class:</b> ");
			appendResolvedOrText(builder, mainWindow, superName);
			builder.append("</li>");
		}

		List<String> interfaces = data.getInterfacesTypes();
		if (!interfaces.isEmpty()) {
			builder.append("<li><b>Interfaces:</b> ");
			for (int i = 0; i < interfaces.size(); i++) {
				if (i > 0) {
					builder.append(", ");
				}
				String ifaceName = Utils.cleanObjectName(interfaces.get(i));
				appendResolvedOrText(builder, mainWindow, ifaceName);
			}
			builder.append("</li>");
		}

		addLine(builder, "Fields count", Integer.toString(data.getFieldsCount()));
		addLine(builder, "Methods count", Integer.toString(data.getMethodsCount()));

		String sourceFile = data.getSourceFile();
		if (sourceFile != null) {
			addLine(builder, "Source file", sourceFile);
		}
		addLine(builder, "File size", fileSize + " B");

		builder.append("</ul>");
		builder.append("</body></html>");
		return builder.toString();
	}

	private static void addLine(StringEscapeUtils.Builder builder, String label, String value) {
		builder.append("<li><b>").escape(label).append(":</b> ").escape(value).append("</li>");
	}

	private void appendResolvedOrText(StringEscapeUtils.Builder builder, MainWindow mainWindow, String rawClsName) {
		JClass cls = resolveJClass(mainWindow, rawClsName);
		if (cls != null) {
			appendClassLink(builder, cls, cls.getFullName());
		} else {
			builder.escape(rawClsName);
		}
	}

	private void appendClassLink(StringEscapeUtils.Builder builder, JClass cls, String text) {
		int linkIdx = links.size();
		links.add(cls);
		builder.append("<a href=\"").append(Integer.toString(linkIdx)).append("\">");
		builder.escape(text);
		builder.append("</a>");
	}

	private static String buildErrorHtml(Exception e) {
		StringEscapeUtils.Builder builder = StringEscapeUtils.builder(StringEscapeUtils.ESCAPE_HTML4);
		builder.append("<html><body>");
		builder.append("<h2>Failed to parse Java class file</h2>");
		builder.append("<pre>");
		builder.escape(Utils.getStackTrace(e));
		builder.append("</pre>");
		builder.append("</body></html>");
		return builder.toString();
	}

	private static @Nullable JClass resolveJClass(MainWindow mainWindow, String rawName) {
		RootNode rootNode = mainWindow.getWrapper().getRootNode();
		if (rootNode == null) {
			return null;
		}
		ClassNode clsNode = rootNode.resolveRawClass(rawName);
		if (clsNode == null) {
			return null;
		}
		return (JClass) mainWindow.getCacheObject().getNodeCache().makeFrom(clsNode);
	}

	private static byte[] loadBytes(JResource res) throws JadxException {
		ResourceFile resFile = res.getResFile();
		ResContainer resContainer = resFile.loadContent();
		if (resContainer != null && resContainer.getDataType() == ResContainer.DataType.DECODED_DATA) {
			return resContainer.getDecodedData();
		}
		return ResourcesLoader.decodeStream(resFile, (size, is) -> is.readAllBytes());
	}

	@Override
	public void loadSettings() {
		htmlPanel.loadSettings();
	}

	private static final class InfoHtmlNode extends JNode {
		private static final long serialVersionUID = 5849102938471029384L;

		private final String name;
		private final ICodeInfo codeInfo;

		InfoHtmlNode(String name, String html) {
			this.name = name;
			this.codeInfo = new SimpleCodeInfo(html);
		}

		@Override
		public ICodeInfo getCodeInfo() {
			return codeInfo;
		}

		@Override
		public JClass getJParent() {
			return null;
		}

		@Override
		public Icon getIcon() {
			return null;
		}

		@Override
		public String makeString() {
			return name;
		}
	}

	private static final class DisasmNode extends JNode {
		private static final long serialVersionUID = -4920192837465102938L;

		private final String name;
		private final @Nullable JavaClassData clsData;
		private @Nullable ICodeInfo cachedCodeInfo;

		DisasmNode(String name, @Nullable JavaClassData clsData) {
			this.name = name;
			this.clsData = clsData;
		}

		@Override
		public synchronized ICodeInfo getCodeInfo() {
			if (cachedCodeInfo == null) {
				cachedCodeInfo = new SimpleCodeInfo(loadDisasm());
			}
			return cachedCodeInfo;
		}

		private String loadDisasm() {
			if (clsData == null) {
				return "Failed to load class data";
			}
			try {
				return clsData.getDisassembledCode();
			} catch (Exception e) {
				return "Failed to disassemble class:\n" + Utils.getStackTrace(e);
			}
		}

		@Override
		public String getSyntaxName() {
			return AbstractCodeArea.SYNTAX_STYLE_SMALI;
		}

		@Override
		public JClass getJParent() {
			return null;
		}

		@Override
		public Icon getIcon() {
			return null;
		}

		@Override
		public String makeString() {
			return name;
		}
	}
}
