package jadx.gui.ui;

import java.util.Arrays;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jetbrains.annotations.Nullable;

import jadx.gui.JadxWrapper;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JPackage;
import jadx.gui.utils.NLS;

class JPackagePopupMenu extends JPopupMenu {
	private static final long serialVersionUID = -7781009781149224131L;

	private final transient MainWindow mainWindow;

	public JPackagePopupMenu(MainWindow mainWindow, JPackage pkg) {
		this.mainWindow = mainWindow;

		add(makeExcludeItem(pkg));
		JMenuItem menuItem = makeRenameMenuItem(pkg);
		if (menuItem != null) {
			add(menuItem);
		}
	}

	@Nullable
	private JMenuItem makeRenameMenuItem(JPackage pkg) {
		List<String> aliasParts = splitPackage(pkg.getName());
		int count = aliasParts.size();
		if (count == 0) {
			return null;
		}
		String rawPackage = getRawPackage(pkg);
		if (rawPackage == null) {
			return null;
		}
		if (count == 1) {
			// single case => no submenu
			String aliasPkg = aliasParts.get(0);
			JPackage renamePkg = new JPackage(rawPackage, aliasPkg);
			JMenuItem pkgItem = new JMenuItem(NLS.str("popup.rename"));
			pkgItem.addActionListener(e -> rename(renamePkg));
			return pkgItem;
		}
		List<String> rawParts = splitPackage(rawPackage); // can be longer then alias
		JMenuItem renameSubMenu = new JMenu(NLS.str("popup.rename"));
		for (int i = 0; i < count; i++) {
			String rawPkg = concat(rawParts, i);
			String aliasShortPkg = aliasParts.get(i);
			JPackage pkgPart = new JPackage(rawPkg, aliasShortPkg);
			JMenuItem pkgPartItem = new JMenuItem(aliasShortPkg);
			pkgPartItem.addActionListener(e -> rename(pkgPart));
			renameSubMenu.add(pkgPartItem);
		}
		return renameSubMenu;
	}

	private String concat(List<String> parts, int n) {
		if (n == 0) {
			return parts.get(0);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(parts.get(0));
		int count = parts.size();
		for (int i = 1; i < count && i <= n; i++) {
			sb.append('.');
			sb.append(parts.get(i));
		}
		return sb.toString();
	}

	private void rename(JPackage pkgPart) {
		new RenameDialog(mainWindow, pkgPart).setVisible(true);
	}

	private List<String> splitPackage(String rawPackage) {
		return Arrays.asList(rawPackage.split("\\."));
	}

	private String getRawPackage(JPackage pkg) {
		for (JClass cls : pkg.getClasses()) {
			return cls.getRootClass().getCls().getClassNode().getClassInfo().getPackage();
		}
		for (JPackage innerPkg : pkg.getInnerPackages()) {
			String rawPackage = getRawPackage(innerPkg);
			if (rawPackage != null) {
				return rawPackage;
			}
		}
		return null;
	}

	private JMenuItem makeExcludeItem(JPackage pkg) {
		JMenuItem excludeItem = new JCheckBoxMenuItem(NLS.str("popup.exclude"));
		excludeItem.setSelected(!pkg.isEnabled());
		excludeItem.addItemListener(e -> {
			JadxWrapper wrapper = mainWindow.getWrapper();
			String fullName = pkg.getFullName();
			if (excludeItem.isSelected()) {
				wrapper.addExcludedPackage(fullName);
			} else {
				wrapper.removeExcludedPackage(fullName);
			}
			mainWindow.reOpenFile();
		});
		return excludeItem;
	}
}
