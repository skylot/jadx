package jadx.gui.ui.codearea;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.treemodel.JNode;

public final class SmaliArea extends RSyntaxTextArea {
	private static final long serialVersionUID = 1334485631870306494L;

	private static final Logger LOG = LoggerFactory.getLogger(SmaliArea.class);

	private final CodePanel contentPanel;
	private final JNode node;

	SmaliArea(CodePanel panel) {
		this.contentPanel = panel;
		this.node = panel.getNode();

		setEditable(false);
		setText(node.getSmali());
	}
}
