package jadx.gui.plugins.apiscan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.event.HyperlinkEvent;

import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeInfo;
import jadx.api.impl.SimpleCodeInfo;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.Utils;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.panel.HtmlPanel;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.utils.UiUtils;

public class ApiScanNode extends JNode {
	private static final long serialVersionUID = 2381706245318843452L;

	private static final ImageIcon ICON = UiUtils.openSvgIcon("ui/warning");

	private final MainWindow mainWindow;
	private final List<MethodNode> links = new ArrayList<>();
	private final String html;

	public static @Nullable ApiScanNode build(MainWindow mainWindow) {
		ApiScanResult result = mainWindow.getWrapper().getPluginApiScan().getResult();
		if (result == null) {
			return null;
		}
		return new ApiScanNode(mainWindow, result);
	}

	private ApiScanNode(MainWindow mainWindow, ApiScanResult result) {
		this.mainWindow = mainWindow;
		this.html = buildHtml(result);
	}

	private String buildHtml(ApiScanResult result) {
		StringEscapeUtils.Builder builder = StringEscapeUtils.builder(StringEscapeUtils.ESCAPE_HTML4);
		builder.append("<html><body>");
		builder.append("<h2>Dangerous API usage</h2>");
		builder.append("<p>Opened file is a jadx plugin, check these calls during plugin review.<br>");
		builder.append("This is a simple scan: it can miss calls hidden by reflection or obfuscation,"
				+ " and calls from bundled libraries are included.</p>");
		Map<ApiCategory, Map<String, List<MethodNode>>> usages = groupUsages(result);
		if (usages.isEmpty()) {
			builder.append("<p>No dangerous API calls found</p>");
		}
		usages.forEach((category, apiMap) -> {
			builder.append("<h3>").escape(category.getTitle()).append("</h3>");
			builder.append("<ul>");
			apiMap.forEach((api, methods) -> {
				builder.append("<li>").escape(api);
				builder.append("<ul>");
				for (MethodNode mth : methods) {
					builder.append("<li><a href=\"").append(Integer.toString(links.size())).append("\">");
					builder.escape(formatMethod(mth));
					builder.append("</a></li>");
					links.add(mth);
				}
				builder.append("</ul></li>");
			});
			builder.append("</ul>");
		});
		builder.append("</body></html>");
		return builder.toString();
	}

	private static Map<ApiCategory, Map<String, List<MethodNode>>> groupUsages(ApiScanResult result) {
		Map<ApiCategory, Map<String, List<MethodNode>>> map = new EnumMap<>(ApiCategory.class);
		for (ApiUsage usage : result.getUsages()) {
			map.computeIfAbsent(usage.getCategory(), c -> new TreeMap<>())
					.computeIfAbsent(usage.getApi(), a -> new ArrayList<>())
					.add(usage.getMth());
		}
		map.values().forEach(apiMap -> apiMap.values()
				.forEach(methods -> methods.sort(Comparator.comparing(ApiScanNode::formatMethod))));
		return map;
	}

	private static String formatMethod(MethodNode mth) {
		return mth.getParentClass().getFullName() + '.' + mth.getAlias()
				+ '(' + Utils.listToString(mth.getArgTypes()) + ')';
	}

	@Override
	public ICodeInfo getCodeInfo() {
		return new SimpleCodeInfo(html);
	}

	@Override
	public boolean hasContent() {
		return true;
	}

	@Override
	public ContentPanel getContentPanel(TabbedPane tabbedPane) {
		HtmlPanel panel = new HtmlPanel(tabbedPane, this);
		panel.getHtmlArea().addHyperlinkListener(event -> {
			if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				MethodNode mth = links.get(Integer.parseInt(event.getDescription()));
				JNode node = mainWindow.getCacheObject().getNodeCache().makeFrom(mth);
				tabbedPane.getTabsController().codeJump(node);
			}
		});
		return panel;
	}

	@Override
	public String makeString() {
		return "Dangerous API usage";
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
