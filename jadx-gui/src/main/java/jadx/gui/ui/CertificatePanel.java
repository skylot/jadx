package jadx.gui.ui;

import java.awt.*;

import javax.swing.*;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.codearea.CodeArea;

public final class CertificatePanel extends ContentPanel {
	private static final long serialVersionUID = 8566591625905036877L;

	private final RSyntaxTextArea textArea;

	CertificatePanel(TabbedPane panel, JNode jnode) {
		super(panel, jnode);
		setLayout(new BorderLayout());
		textArea = new RSyntaxTextArea(jnode.getContent());
		loadSettings();
		JScrollPane sp = new JScrollPane(textArea);
		add(sp);
	}

	@Override
	public void loadSettings() {
		CodeArea.loadCommonSettings(getTabbedPane().getMainWindow(), textArea);
	}
}
