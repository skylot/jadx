package jadx.gui.ui.popupmenu;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;

public class RecentProjectsMenuListener implements MenuListener {
	private final MainWindow mainWindow;
	private final JMenu menu;

	public RecentProjectsMenuListener(MainWindow mainWindow, JMenu menu) {
		this.mainWindow = mainWindow;
		this.menu = menu;
	}

	@Override
	public void menuSelected(MenuEvent menuEvent) {
		Set<Path> current = new HashSet<>(mainWindow.getProject().getFilePaths());
		List<JMenuItem> items = mainWindow.getSettings().getRecentProjects()
				.stream()
				.filter(path -> !current.contains(path))
				.map(path -> {
					JMenuItem menuItem = new JMenuItem(path.toAbsolutePath().toString());
					menuItem.addActionListener(e -> mainWindow.open(Collections.singletonList(path)));
					return menuItem;
				}).collect(Collectors.toList());

		menu.removeAll();
		if (items.isEmpty()) {
			menu.add(new JMenuItem(NLS.str("menu.no_recent_projects")));
		} else {
			items.forEach(menu::add);
		}
	}

	@Override
	public void menuDeselected(MenuEvent e) {
	}

	@Override
	public void menuCanceled(MenuEvent e) {
	}
}
