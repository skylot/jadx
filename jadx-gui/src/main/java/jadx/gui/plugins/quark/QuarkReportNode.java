package jadx.gui.plugins.quark;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jadx.api.ICodeInfo;
import jadx.api.impl.SimpleCodeInfo;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.TabbedPane;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.panel.HtmlPanel;
import jadx.gui.utils.UiUtils;

public class QuarkReportNode extends JNode {

	private static final long serialVersionUID = -766800957202637021L;

	private static final Logger LOG = LoggerFactory.getLogger(QuarkReportNode.class);

	private static final Gson GSON = new GsonBuilder().create();

	private static final ImageIcon ICON = UiUtils.openSvgIcon("ui/quark");

	private final Path reportFile;

	private ICodeInfo errorContent;

	public QuarkReportNode(Path reportFile) {
		this.reportFile = reportFile;
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	@Override
	public Icon getIcon() {
		return ICON;
	}

	@Override
	public String makeString() {
		return "Quark analysis report";
	}

	@Override
	public ContentPanel getContentPanel(TabbedPane tabbedPane) {
		try {
			QuarkReportData data;
			try (BufferedReader reader = Files.newBufferedReader(reportFile)) {
				data = GSON.fromJson(reader, QuarkReportData.class);
			}
			data.validate();
			return new QuarkReportPanel(tabbedPane, this, data);
		} catch (Exception e) {
			LOG.error("Quark report parse error", e);
			StringEscapeUtils.Builder builder = StringEscapeUtils.builder(StringEscapeUtils.ESCAPE_HTML4);
			builder.append("<h2>");
			builder.escape("Quark analysis failed!");
			builder.append("</h2>");
			builder.append("<h3>");
			builder.append("Error: ").escape(e.getMessage());
			builder.append("</h3>");
			builder.append("<pre>");
			builder.escape(ExceptionUtils.getStackTrace(e));
			builder.append("</pre>");
			errorContent = new SimpleCodeInfo(builder.toString());
			return new HtmlPanel(tabbedPane, this);
		}
	}

	@Override
	public ICodeInfo getCodeInfo() {
		return errorContent;
	}
}
