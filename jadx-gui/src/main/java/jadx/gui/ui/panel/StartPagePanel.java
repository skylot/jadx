package jadx.gui.ui.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.nio.file.Path;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import jadx.gui.settings.JadxSettings;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.TabbedPane;
import jadx.gui.ui.treenodes.StartPageNode;
import jadx.gui.utils.Icons;
import jadx.gui.utils.NLS;

public class StartPagePanel extends ContentPanel {

	public StartPagePanel(TabbedPane tabbedPane, StartPageNode node) {
		super(tabbedPane, node);
		MainWindow mainWindow = tabbedPane.getMainWindow();
		Font baseFont = mainWindow.getSettings().getFont();

		JButton openFile = new JButton(NLS.str("file.open_title"), Icons.OPEN);
		openFile.addActionListener(ev -> mainWindow.openFileDialog());

		JButton openProject = new JButton(NLS.str("file.open_project"), Icons.OPEN_PROJECT);
		openProject.addActionListener(ev -> mainWindow.openProjectDialog());

		JPanel start = new JPanel();
		start.setBorder(sectionFrame(NLS.str("start_page.start"), baseFont));
		start.setLayout(new BoxLayout(start, BoxLayout.LINE_AXIS));
		start.add(openFile);
		start.add(Box.createRigidArea(new Dimension(10, 0)));
		start.add(openProject);
		start.add(Box.createHorizontalGlue());

		JPanel recentPanel = new JPanel();
		JScrollPane scrollPane = new JScrollPane(recentPanel);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(400, 200));
		scrollPane.setBorder(BorderFactory.createEmptyBorder());

		fillRecentPanel(recentPanel, scrollPane, mainWindow);

		JPanel recent = new JPanel();
		recent.setBorder(sectionFrame(NLS.str("start_page.recent"), baseFont));
		recent.setLayout(new BoxLayout(recent, BoxLayout.PAGE_AXIS));
		recent.add(scrollPane);

		JPanel center = new JPanel();
		center.setLayout(new BorderLayout(10, 10));
		center.add(start, BorderLayout.PAGE_START);
		center.add(recent, BorderLayout.CENTER);
		center.setMaximumSize(new Dimension(700, 600));
		center.setAlignmentX(CENTER_ALIGNMENT);

		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
		add(Box.createVerticalGlue());
		add(center);
		add(Box.createVerticalGlue());
	}

	private void fillRecentPanel(JPanel panel, JScrollPane scrollPane, MainWindow mainWindow) {
		JadxSettings settings = mainWindow.getSettings();
		List<Path> recentProjects = settings.getRecentProjects();
		panel.removeAll();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		Font baseFont = settings.getFont();
		Font font = baseFont.deriveFont(baseFont.getSize() - 1f);
		for (Path path : recentProjects) {
			JButton openBtn = new JButton(path.getFileName().toString());
			openBtn.setToolTipText(path.toAbsolutePath().toString());
			openBtn.setFont(font);
			openBtn.setBorderPainted(false);
			openBtn.addActionListener(ev -> mainWindow.open(path));

			JButton removeBtn = new JButton();
			removeBtn.setIcon(Icons.CLOSE_INACTIVE);
			removeBtn.setRolloverIcon(Icons.CLOSE);
			removeBtn.setRolloverEnabled(true);
			removeBtn.setFocusable(false);
			removeBtn.setBorder(null);
			removeBtn.setBorderPainted(false);
			removeBtn.setContentAreaFilled(false);
			removeBtn.setOpaque(true);
			removeBtn.addActionListener(e -> {
				mainWindow.getSettings().removeRecentProject(path);
				fillRecentPanel(panel, scrollPane, mainWindow);
				panel.revalidate();
				scrollPane.repaint();
			});
			JPanel linePanel = new JPanel();
			linePanel.setLayout(new BoxLayout(linePanel, BoxLayout.LINE_AXIS));
			linePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			linePanel.add(openBtn);
			linePanel.add(Box.createHorizontalGlue());
			linePanel.add(removeBtn);

			panel.add(linePanel);
		}
		panel.add(Box.createVerticalGlue());
	}

	private static Border sectionFrame(String title, Font font) {
		TitledBorder titledBorder = BorderFactory.createTitledBorder(title);
		titledBorder.setTitleFont(font.deriveFont(Font.BOLD, font.getSize() + 1));
		Border spacing = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		return BorderFactory.createCompoundBorder(titledBorder, spacing);
	}

	@Override
	public void loadSettings() {
	}
}
