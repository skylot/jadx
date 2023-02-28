package jadx.gui.treemodel;

import java.nio.file.Path;

import javax.swing.Icon;
import javax.swing.JPopupMenu;

import jadx.gui.ui.MainWindow;
import jadx.gui.utils.Icons;
import jadx.gui.utils.NLS;
import jadx.gui.utils.ui.SimpleMenuItem;

public class JInputFile extends JNode {

	private final Path filePath;

	public JInputFile(Path filePath) {
		this.filePath = filePath;
	}

	@Override
	public JPopupMenu onTreePopupMenu(MainWindow mainWindow) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(new SimpleMenuItem(NLS.str("popup.add_files"), mainWindow::addFiles));
		menu.add(new SimpleMenuItem(NLS.str("popup.remove"), () -> mainWindow.removeInput(filePath)));
		return menu;
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	@Override
	public Icon getIcon() {
		return Icons.FILE;
	}

	@Override
	public String makeString() {
		return filePath.getFileName().toString();
	}

	@Override
	public String getTooltip() {
		return filePath.normalize().toAbsolutePath().toString();
	}
}
