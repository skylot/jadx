package jadx.gui.treemodel;

import java.nio.file.Path;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.SimpleMenuItem;

public class JInputScripts extends JNode {
	private static final ImageIcon INPUT_SCRIPTS_ICON = UiUtils.openSvgIcon("nodes/scriptsModel");

	public JInputScripts(List<Path> scripts) {
		for (Path script : scripts) {
			add(new JInputScript(script));
		}
	}

	@Override
	public JPopupMenu onTreePopupMenu(MainWindow mainWindow) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(new SimpleMenuItem(NLS.str("popup.add_scripts"), mainWindow::addFiles));
		menu.add(new SimpleMenuItem(NLS.str("popup.new_script"), mainWindow::addNewScript));
		return menu;
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	@Override
	public Icon getIcon() {
		return INPUT_SCRIPTS_ICON;
	}

	@Override
	public String makeString() {
		return NLS.str("tree.input_scripts");
	}
}
