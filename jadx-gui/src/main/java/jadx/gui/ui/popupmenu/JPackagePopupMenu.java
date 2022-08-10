package jadx.gui.ui.popupmenu;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.JadxWrapper;
import jadx.gui.treemodel.JPackage;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.dialog.ExcludePkgDialog;
import jadx.gui.ui.dialog.RenameDialog;
import jadx.gui.utils.NLS;
import jadx.gui.utils.pkgs.JRenamePackage;
import jadx.gui.utils.pkgs.PackageHelper;

public class JPackagePopupMenu extends JPopupMenu {
	private static final long serialVersionUID = -7781009781149224131L;

	private static final Logger LOG = LoggerFactory.getLogger(JPackagePopupMenu.class);

	private final transient MainWindow mainWindow;

	public JPackagePopupMenu(MainWindow mainWindow, JPackage pkg) {
		this.mainWindow = mainWindow;

		add(makeExcludeItem(pkg));
		add(makeExcludeItem());
		add(makeRenameMenuItem(pkg));
	}

	private JMenuItem makeRenameMenuItem(JPackage pkg) {
		JMenuItem renameSubMenu = new JMenu(NLS.str("popup.rename"));
		PackageHelper packageHelper = mainWindow.getCacheObject().getPackageHelper();
		List<JRenamePackage> nodes = packageHelper.getRenameNodes(pkg);
		for (JRenamePackage node : nodes) {
			JMenuItem pkgPartItem = new JMenuItem(node.getTitle(), node.getIcon());
			pkgPartItem.addActionListener(e -> rename(node));
			renameSubMenu.add(pkgPartItem);
		}
		return renameSubMenu;
	}

	private void rename(JRenamePackage pkg) {
		LOG.debug("Renaming package: {}", pkg);
		RenameDialog.rename(mainWindow, null, pkg);
	}

	private JMenuItem makeExcludeItem(JPackage pkg) {
		JMenuItem excludeItem = new JCheckBoxMenuItem(NLS.str("popup.exclude"));
		excludeItem.setSelected(!pkg.isEnabled());
		excludeItem.addItemListener(e -> {
			JadxWrapper wrapper = mainWindow.getWrapper();
			String fullName = pkg.getPkg().getFullName();
			if (excludeItem.isSelected()) {
				wrapper.addExcludedPackage(fullName);
			} else {
				wrapper.removeExcludedPackage(fullName);
			}
			mainWindow.reopen();
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
