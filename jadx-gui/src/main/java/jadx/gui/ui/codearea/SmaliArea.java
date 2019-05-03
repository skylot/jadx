package jadx.gui.ui.codearea;

import jadx.gui.ui.ContentPanel;

import jadx.gui.treemodel.JNode;

public final class SmaliArea extends AbstractCodeArea {
	private static final long serialVersionUID = 1334485631870306494L;

	private final JNode node;

	SmaliArea(ContentPanel contentPanel) {
		super(contentPanel);
		node = contentPanel.getNode();

		setEditable(false);
	}

	@Override
	public void load() {
		if (getText().isEmpty()) {
			setText(node.getSmali());
			setCaretPosition(0);
		}
	}
}
