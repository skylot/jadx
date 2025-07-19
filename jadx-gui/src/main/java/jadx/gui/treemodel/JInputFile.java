package jadx.gui.treemodel;

import java.nio.file.Path;
import java.util.Objects;

import javax.swing.Icon;
import javax.swing.JPopupMenu;

import jadx.gui.ui.MainWindow;
import jadx.gui.utils.Icons;
import jadx.gui.utils.NLS;
import jadx.gui.utils.ui.SimpleMenuItem;

public class JInputFile extends JNode {

	private final Path filePath;

	public JInputFile(Path filePath) {
		this.filePath = Objects.requireNonNull(filePath);
	}

	@Override
	public JPopupMenu onTreePopupMenu(MainWindow mainWindow) {
		return buildInputFilePopupMenu(mainWindow, filePath);
	}

	public static JPopupMenu buildInputFilePopupMenu(MainWindow mainWindow, Path filePath) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(new SimpleMenuItem(NLS.str("popup.add_files"), mainWindow::addFiles));
		menu.add(new SimpleMenuItem(NLS.str("popup.remove"), () -> mainWindow.removeInput(filePath)));
		menu.add(new SimpleMenuItem(NLS.str("popup.rename"), () -> mainWindow.renameInput(filePath)));
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

	@Override
	public int hashCode() {
		return filePath.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		return ((JInputFile) o).filePath.equals(filePath);
	}

	@Override
	public String toString() {
		return "JInputFile{" + filePath + '}';
	}
}
