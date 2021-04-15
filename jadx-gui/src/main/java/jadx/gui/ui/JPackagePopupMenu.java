package jadx.gui.ui;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.JadxWrapper;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JPackage;
import jadx.gui.utils.NLS;

class JPackagePopupMenu extends JPopupMenu {
	private static final long serialVersionUID = -7781009781149224131L;

	private static final Logger LOG = LoggerFactory.getLogger(JPackagePopupMenu.class);

	private final transient MainWindow mainWindow;

	public JPackagePopupMenu(MainWindow mainWindow, JPackage pkg) {
		this.mainWindow = mainWindow;

		add(makeExcludeItem(pkg));
		add(makeExcludeItem());
		JMenuItem menuItem = makeRenameMenuItem(pkg);
		if (menuItem != null) {
			add(menuItem);
		}
	}

	@Nullable
	private JMenuItem makeRenameMenuItem(JPackage pkg) {
		List<String> aliasShortParts = splitPackage(pkg.getName());
		int count = aliasShortParts.size();
		if (count == 0) {
			return null;
		}
		String rawPackage = getRawPackage(pkg);
		if (rawPackage == null) {
			return null;
		}
		List<String> aliasParts = splitPackage(pkg.getFullName());
		List<String> rawParts = splitPackage(rawPackage); // can be longer then alias parts
		int start = aliasParts.size() - count;
		if (count == 1) {
			// single case => no submenu
			JPackage renamePkg = new JPackage(concat(rawParts, start), aliasParts.get(start));
			JMenuItem pkgItem = new JMenuItem(NLS.str("popup.rename"));
			pkgItem.addActionListener(e -> rename(renamePkg));
			return pkgItem;
		}
		JMenuItem renameSubMenu = new JMenu(NLS.str("popup.rename"));
		for (int i = start; i < aliasParts.size(); i++) {
			String aliasShortPkg = aliasParts.get(i);
			JPackage pkgPart = new JPackage(concat(rawParts, i), aliasShortPkg);
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

	private void rename(JPackage pkg) {
		LOG.debug("Renaming package: fullName={}, name={}", pkg.getFullName(), pkg.getName());
		RenameDialog.rename(mainWindow, pkg);
	}

	private List<String> splitPackage(String rawPackage) {
		return Arrays.asList(rawPackage.split("\\."));
	}

	private String getRawPackage(JPackage pkg) {
		List<JClass> classes = pkg.getClasses();
		if (!classes.isEmpty()) {
			return classes.get(0).getRootClass().getCls().getClassNode().getClassInfo().getPackage();
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

	private JMenuItem makeExcludeItem() {
		return new JMenuItem(new AbstractAction(NLS.str("popup.exclude_packages")) {
			private static final long serialVersionUID = -1111111202104151028L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new ExcludePkgDialog(mainWindow).setVisible(true);
			}
		});
	}
}
