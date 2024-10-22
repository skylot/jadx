package jadx.gui.settings.ui.font;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.drjekyll.fontchooser.FontChooser;
import org.jetbrains.annotations.Nullable;

import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.ui.JadxSettingsWindow;
import jadx.gui.utils.NLS;

public class JadxFontDialog extends JDialog {
	private static final long serialVersionUID = 7609857698785777587L;

	private final FontChooser fontChooser = new FontChooser();
	private final JadxSettings settings;
	private boolean selected = false;

	public JadxFontDialog(JadxSettingsWindow settingsWindow, String title) {
		super(settingsWindow, title, true);
		settings = settingsWindow.getMainWindow().getSettings();
		initComponents();
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		if (!settings.loadWindowPos(this)) {
			pack();
		}
	}

	public @Nullable Font select(Font currentFont, boolean onlyMonospace) {
		fontChooser.setSelectedFont(currentFont);
		if (onlyMonospace) {
			FontChooserHack.setOnlyMonospace(fontChooser);
		}
		setVisible(true);
		Font selectedFont = fontChooser.getSelectedFont();
		if (selected && !selectedFont.equals(currentFont)) {
			return selectedFont;
		}
		return null;
	}

	private void initComponents() {
		JPanel chooserPanel = new JPanel();
		chooserPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		chooserPanel.setLayout(new BorderLayout(0, 10));
		chooserPanel.add(fontChooser);

		JPanel controlPanel = new JPanel();
		controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		controlPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));

		JButton okBtn = new JButton();
		okBtn.setText(NLS.str("common_dialog.ok"));
		okBtn.setMnemonic('o');
		okBtn.addActionListener(event -> {
			selected = true;
			dispose();
		});

		JButton cancelBtn = new JButton();
		cancelBtn.setText(NLS.str("common_dialog.cancel"));
		cancelBtn.setMnemonic('c');
		cancelBtn.addActionListener(event -> dispose());

		controlPanel.add(okBtn);
		controlPanel.add(cancelBtn);

		add(chooserPanel);
		add(controlPanel, BorderLayout.PAGE_END);
		getRootPane().setDefaultButton(okBtn);
	}

	@Override
	public void dispose() {
		settings.saveWindowPos(this);
		super.dispose();
	}
}
