package jadx.gui.ui;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.utils.UiUtils;

public class QuarkReport extends JNode {

	private static final long serialVersionUID = -766800957202637021L;

	private static final Logger LOG = LoggerFactory.getLogger(QuarkReport.class);

	private static final ImageIcon REPORT_ICON = UiUtils.openIcon("report");

	private String content;
	private String apkFileName;

	private JsonObject reportData;

	public static QuarkReport analysisAPK(JsonObject data) {
		return new QuarkReport(data);
	}

	public QuarkReport(JsonObject data) {
		this.reportData = data;
		this.apkFileName = data.get("apk_filename").getAsString();
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	@Override
	public Icon getIcon() {
		return REPORT_ICON;
	}

	@Override
	public String makeString() {
		return "Quark analysis report";
	}

	@Override
	public String getContent() {
		if (content != null) {
			return this.content;
		}
		try {

			JsonArray crimes = (JsonArray) this.reportData.get("crimes");

			StringEscapeUtils.Builder builder = StringEscapeUtils.builder(StringEscapeUtils.ESCAPE_HTML4);

			builder.append("<h1>Quark Analysis Report</h1>");
			builder.append("<h3>");
			builder.append("File name: ");
			builder.append(apkFileName);
			builder.append("</h3>");
			builder.append("<table><thead><tr>");
			builder.append("<th>Potential Malicious Activities</th>");
			builder.append("<th>Confidence</th>");
			builder.append("</tr></thead><tbody>");

			for (Object obj : crimes) {
				JsonObject crime = (JsonObject) obj;
				String crimeDes = crime.get("crime").getAsString();
				String confidence = crime.get("confidence").getAsString();

				builder.append("<tr><td>");
				builder.append(crimeDes);
				builder.append("</td><td>");
				builder.append(confidence);
				builder.append("</td></tr>");
			}

			builder.append("</tbody></table>");
			this.content = builder.toString();

		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			StringEscapeUtils.Builder builder = StringEscapeUtils.builder(StringEscapeUtils.ESCAPE_HTML4);
			builder.append("<h1>");
			builder.escape("Quark analysis failed!");
			builder.append("</h1><pre>");
			builder.escape(ExceptionUtils.getStackTrace(e));
			builder.append("</pre>");
			return builder.toString();
		}

		return this.content;
	}

}
