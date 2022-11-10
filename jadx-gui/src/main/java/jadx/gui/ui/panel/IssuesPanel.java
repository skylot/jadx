package jadx.gui.ui.panel;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ch.qos.logback.classic.Level;

import jadx.gui.logs.IssuesListener;
import jadx.gui.logs.LogCollector;
import jadx.gui.logs.LogOptions;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class IssuesPanel extends JPanel {
	private static final long serialVersionUID = -7720576036668459218L;

	private static final ImageIcon ERROR_ICON = UiUtils.openSvgIcon("ui/error");
	private static final ImageIcon WARN_ICON = UiUtils.openSvgIcon("ui/warning");

	private final MainWindow mainWindow;
	private final IssuesListener issuesListener;
	private JLabel errorLabel;
	private JLabel warnLabel;

	public IssuesPanel(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		initUI();
		this.issuesListener = new IssuesListener(this);
		LogCollector.getInstance().registerListener(issuesListener);
	}

	public int getErrorsCount() {
		return issuesListener.getErrors();
	}

	private void initUI() {
		JLabel label = new JLabel(NLS.str("issues_panel.label"));
		errorLabel = new JLabel(ERROR_ICON);
		warnLabel = new JLabel(WARN_ICON);

		String toolTipText = NLS.str("issues_panel.tooltip");
		errorLabel.setToolTipText(toolTipText);
		warnLabel.setToolTipText(toolTipText);

		errorLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				mainWindow.showLogViewer(LogOptions.allWithLevel(Level.ERROR));
			}
		});
		warnLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				mainWindow.showLogViewer(LogOptions.allWithLevel(Level.WARN));
			}
		});

		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setVisible(false);
		add(label);
		add(Box.createHorizontalGlue());
		add(errorLabel);
		add(Box.createHorizontalGlue());
		add(warnLabel);
	}

	public void onUpdate(int error, int warnings) {
		if (error == 0 && warnings == 0) {
			setVisible(false);
			return;
		}
		setVisible(true);
		errorLabel.setText(NLS.str("issues_panel.errors", error));
		errorLabel.setVisible(error != 0);
		warnLabel.setText(NLS.str("issues_panel.warnings", warnings));
		warnLabel.setVisible(warnings != 0);
	}
}
