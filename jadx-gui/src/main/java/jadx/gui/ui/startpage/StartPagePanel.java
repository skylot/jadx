package jadx.gui.ui.startpage;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.nio.file.Path;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import jadx.gui.settings.JadxSettings;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.utils.Icons;
import jadx.gui.utils.NLS;

public class StartPagePanel extends ContentPanel {
	private static final long serialVersionUID = 2457805175218770732L;

	private final RecentProjectsJList recentList;
	private final DefaultListModel<RecentProjectItem> recentListModel;
	private final MainWindow mainWindow;
	private final JadxSettings settings;

	public static int hoveredRemoveBtnIndex = -1;

	public StartPagePanel(TabbedPane tabbedPane, StartPageNode node) {
		super(tabbedPane, node);
		this.mainWindow = tabbedPane.getMainWindow();
		this.settings = mainWindow.getSettings();
		Font baseFont = settings.getFont();

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

		this.recentListModel = new DefaultListModel<>();
		this.recentList = new RecentProjectsJList(recentListModel);
		this.recentList.setCellRenderer(new RecentProjectListCellRenderer(baseFont));
		this.recentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane scrollPane = new JScrollPane(recentList);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(400, 250));
		scrollPane.setBorder(BorderFactory.createEmptyBorder());

		recentList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int index = recentList.locationToIndex(e.getPoint());
				if (index == -1) {
					return;
				}

				RecentProjectItem item = recentListModel.getElementAt(index);
				if (item == null) {
					return;
				}

				RecentProjectListCellRenderer renderer = (RecentProjectListCellRenderer) recentList.getCellRenderer()
						.getListCellRendererComponent(recentList, item, index, false, false);

				Rectangle cellBounds = recentList.getCellBounds(index, index);
				if (cellBounds != null) {
					int xInCell = e.getX() - cellBounds.x;
					int yInCell = e.getY() - cellBounds.y;

					Rectangle removeIconBounds = renderer.getRemoveIconBounds();
					if (removeIconBounds != null && removeIconBounds.contains(xInCell, yInCell)) {
						removeRecentProject(item.getPath());
						return;
					}
				}

				if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
					openRecentProject(item.getPath());
				} else if (SwingUtilities.isRightMouseButton(e)) {
					recentList.setSelectedIndex(index);
					showRecentProjectContextMenu(e);
				}
			}
		});

		recentList.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				int oldHoveredRemoveBtnIndex = hoveredRemoveBtnIndex;
				hoveredRemoveBtnIndex = -1;

				int currentCellIndex = recentList.locationToIndex(e.getPoint());

				if (currentCellIndex != -1) {
					RecentProjectItem item = recentListModel.getElementAt(currentCellIndex);
					RecentProjectListCellRenderer renderer = (RecentProjectListCellRenderer) recentList.getCellRenderer()
							.getListCellRendererComponent(recentList, item, currentCellIndex, recentList.isSelectedIndex(currentCellIndex),
									false);

					Rectangle cellBounds = recentList.getCellBounds(currentCellIndex, currentCellIndex);
					if (cellBounds != null) {
						int xInCell = e.getX() - cellBounds.x;
						int yInCell = e.getY() - cellBounds.y;

						Rectangle removeIconBounds = renderer.getRemoveIconBounds();
						if (removeIconBounds != null && removeIconBounds.contains(xInCell, yInCell)) {
							hoveredRemoveBtnIndex = currentCellIndex;
						}
					}
				}

				if (oldHoveredRemoveBtnIndex != hoveredRemoveBtnIndex) {
					if (oldHoveredRemoveBtnIndex != -1) {
						Rectangle bounds = recentList.getCellBounds(oldHoveredRemoveBtnIndex, oldHoveredRemoveBtnIndex);
						if (bounds != null) {
							recentList.repaint(bounds);
						}
					}
					if (hoveredRemoveBtnIndex != -1) {
						Rectangle bounds = recentList.getCellBounds(hoveredRemoveBtnIndex, hoveredRemoveBtnIndex);
						if (bounds != null) {
							recentList.repaint(bounds);
						}
					}
				}
			}
		});

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

		fillRecentProjectsList();
	}

	private void fillRecentProjectsList() {
		recentListModel.clear();
		List<Path> recentPaths = settings.getRecentProjects();
		for (Path path : recentPaths) {
			recentListModel.addElement(new RecentProjectItem(path));
		}
		recentList.revalidate();
		recentList.repaint();
	}

	private void openRecentProject(Path path) {
		mainWindow.open(path);
	}

	private void removeRecentProject(Path path) {
		settings.removeRecentProject(path);
		fillRecentProjectsList();
		if (hoveredRemoveBtnIndex != -1 && hoveredRemoveBtnIndex >= recentListModel.size()) {
			hoveredRemoveBtnIndex = -1;
		}
	}

	private void showRecentProjectContextMenu(MouseEvent e) {
		JPopupMenu popupMenu = new JPopupMenu();
		RecentProjectItem selectedItem = recentList.getSelectedValue();

		if (selectedItem != null) {
			JMenuItem openItem = new JMenuItem(NLS.str("file.open_project"));
			openItem.addActionListener(actionEvent -> openRecentProject(selectedItem.getPath()));
			popupMenu.add(openItem);

			JMenuItem removeItem = new JMenuItem(NLS.str("start_page.list.delete_recent_project"));
			removeItem.addActionListener(actionEvent -> removeRecentProject(selectedItem.getPath()));
			popupMenu.add(removeItem);
		}

		popupMenu.show(e.getComponent(), e.getX(), e.getY());
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

	/**
	 * Inner class: Custom JList to override getToolTipText method.
	 * This allows displaying specific tooltips based on mouse position within a cell.
	 */
	private static class RecentProjectsJList extends JList<RecentProjectItem> {
		private static final long serialVersionUID = 1L;

		public RecentProjectsJList(DefaultListModel<RecentProjectItem> model) {
			super(model);
		}

		@Override
		public String getToolTipText(MouseEvent event) {
			int index = locationToIndex(event.getPoint());
			if (index == -1) {
				return null;
			}

			RecentProjectItem item = getModel().getElementAt(index);
			if (item == null) {
				return null;
			}

			RecentProjectListCellRenderer renderer = (RecentProjectListCellRenderer) getCellRenderer()
					.getListCellRendererComponent(this, item, index, isSelectedIndex(index), false);

			Rectangle cellBounds = getCellBounds(index, index);
			if (cellBounds != null) {
				int xInCell = event.getX() - cellBounds.x;
				int yInCell = event.getY() - cellBounds.y;

				Rectangle removeIconBounds = renderer.getRemoveIconBounds();
				if (removeIconBounds != null && removeIconBounds.contains(xInCell, yInCell)) {
					return NLS.str("start_page.list.delete_recent_project.tooltip");
				}
			}
			return item.getAbsolutePath();
		}
	}
}
