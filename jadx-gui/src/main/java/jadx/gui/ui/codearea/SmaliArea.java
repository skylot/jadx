package jadx.gui.ui.codearea;

import jadx.gui.ui.ContentPanel;

public final class SmaliArea extends AbstractCodeArea {
	private static final long serialVersionUID = 1334485631870306494L;

	SmaliArea(ContentPanel contentPanel) {
		super(contentPanel);
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
