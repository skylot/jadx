package jadx.gui.utils.ui;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import jadx.gui.treemodel.JNode;

public class NodeLabel extends JLabel {

	public static NodeLabel longName(JNode node) {
		NodeLabel label = new NodeLabel(node.makeLongStringHtml(), node.disableHtml());
		label.setIcon(node.getIcon());
		label.setHorizontalAlignment(SwingConstants.LEFT);
		return label;
	}

	public static NodeLabel noHtml(String label) {
		return new NodeLabel(label, true);
	}

	public static void disableHtml(JLabel label, boolean disable) {
		label.putClientProperty("html.disable", disable);
	}

	private boolean htmlDisabled = false;

	public NodeLabel() {
		disableHtml(true);
	}

	public NodeLabel(String label) {
		disableHtml(true);
		setText(label);
	}

	public NodeLabel(String label, boolean disableHtml) {
		disableHtml(disableHtml);
		setText(label);
	}

	public void disableHtml(boolean disable) {
		if (htmlDisabled != disable) {
			htmlDisabled = disable;
			disableHtml(this, disable);
		}
	}
}
