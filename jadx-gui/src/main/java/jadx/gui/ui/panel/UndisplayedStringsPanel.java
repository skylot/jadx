package jadx.gui.ui.panel;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;

import org.drjekyll.fontchooser.FontChooser;
import org.drjekyll.fontchooser.model.FontSelectionModel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.LineNumbersMode;
import jadx.gui.settings.ui.font.FontChooserHack;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.ui.treenodes.UndisplayedStringsNode;

public class UndisplayedStringsPanel extends ContentPanel {
	private static final long serialVersionUID = 695370628262996993L;

	private final RSyntaxTextArea textPane;
	private final RTextScrollPane codeScrollPane;

	public UndisplayedStringsPanel(TabbedPane panel, UndisplayedStringsNode node) {
		super(panel, node);
		setLayout(new BorderLayout());
		textPane = AbstractCodeArea.getDefaultArea(panel.getMainWindow());

		JadxSettings settings = getSettings();
		Font selectedFont = settings.getFont();

		FontChooser fontChooser = new FontChooser();
		fontChooser.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		fontChooser.setSelectedFont(selectedFont);
		FontChooserHack.hidePreview(fontChooser);

		fontChooser.addChangeListener(event -> {
			FontSelectionModel model = (FontSelectionModel) event.getSource();
			settings.setFont(model.getSelectedFont());
			SwingUtilities.invokeLater(() -> {
				getMainWindow().loadSettings();
			});
		});

		codeScrollPane = new RTextScrollPane(textPane);

		add(codeScrollPane, BorderLayout.CENTER);
		add(fontChooser, BorderLayout.EAST);

		applySettings();
		showData(node.makeDescString());
	}

	private void applySettings() {
		codeScrollPane.setLineNumbersEnabled(getSettings().getLineNumbersMode() != LineNumbersMode.DISABLE);
		codeScrollPane.getGutter().setLineNumberFont(getSettings().getFont());
		textPane.setFont(getSettings().getFont());
	}

	private void showData(String data) {
		textPane.setText(data);
		textPane.setCaretPosition(0);
	}

	@Override
	public void loadSettings() {
		applySettings();
	}

}
