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

import jadx.api.gui.IMainWindow;
import jadx.api.gui.plugins.JadxGuiContextExt;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.Utils;
import jadx.gui.code.HtmlContentPanel;
import jadx.gui.tree.JadxTreeNode;
import jadx.gui.utils.UiUtils;

public class ApiScanNode extends JadxTreeNode {
	private static final long serialVersionUID = 2381706245318843452L;

	private static final ImageIcon ICON = UiUtils.openSvgIcon("ui/warning");

	private final JadxGuiContextExt guiContextExt;
	private final List<MethodNode> links = new ArrayList<>();
	private final String html;

	ApiScanNode(JadxGuiContextExt guiContextExt, ApiScanResult result) {
		this.guiContextExt = guiContextExt;
		this.html = buildHtml(result);
	}

	@Override
	public String getID() {
		return "ApiScanNode";
	}

	@Override
	public String getName() {
		return "Dangerous API usage";
	}

	@Override
	public @Nullable HtmlContentPanel getContentPanel(IMainWindow iMainWindow) {
		HtmlContentPanel panel = new HtmlContentPanel(iMainWindow, this, html);
		panel.getHtmlArea().addHyperlinkListener(event -> {
			if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				guiContextExt.open(links.get(Integer.parseInt(event.getDescription())));
			}
		});
		return panel;
	}

	@Override
	public boolean hasContent() {
		return true;
	}

	private String buildHtml(ApiScanResult result) {
		StringEscapeUtils.Builder builder = StringEscapeUtils.builder(StringEscapeUtils.ESCAPE_HTML4);
		builder.append("<html><body>");
		builder.append("<h1>Dangerous API usage</h1>");
		builder.append("<p>Opened file is a jadx plugin, check these calls during plugin review.<br>");
		builder.append("Calls reachable from plugin entry classes are listed first,"
				+ " but malicious code can be added into any class, so check other code too.<br>");
		builder.append("This is a simple scan: it can miss calls hidden by reflection or obfuscation.</p>");
		List<ApiUsage> reachable = new ArrayList<>();
		List<ApiUsage> other = new ArrayList<>();
		for (ApiUsage usage : result.getUsages()) {
			if (result.isReachable(usage)) {
				reachable.add(usage);
			} else {
				other.add(usage);
			}
		}
		addSection(builder, "Reachable from plugin entry", reachable);
		addSection(builder, "Other code", other);
		builder.append("</body></html>");
		return builder.toString();
	}

	private void addSection(StringEscapeUtils.Builder builder, String title, List<ApiUsage> usages) {
		builder.append("<h2>").escape(title).append("</h2>");
		Map<ApiCategory, Map<String, List<MethodNode>>> groupedUsages = groupUsages(usages);
		if (groupedUsages.isEmpty()) {
			builder.append("<p>No dangerous API calls found</p>");
		}
		groupedUsages.forEach((category, apiMap) -> {
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
	}

	private static Map<ApiCategory, Map<String, List<MethodNode>>> groupUsages(List<ApiUsage> usages) {
		Map<ApiCategory, Map<String, List<MethodNode>>> map = new EnumMap<>(ApiCategory.class);
		for (ApiUsage usage : usages) {
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
	public Icon getIcon() {
		return ICON;
	}
}
