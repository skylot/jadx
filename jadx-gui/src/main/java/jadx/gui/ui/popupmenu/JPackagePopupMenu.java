package jadx.gui.ui.popupmenu;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.JadxWrapper;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JPackage;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.dialog.ExcludePkgDialog;
import jadx.gui.ui.dialog.RenameDialog;
import jadx.gui.ui.filedialog.FileDialogWrapper;
import jadx.gui.ui.filedialog.FileOpenMode;
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
		add(makeExportSubMenu(pkg));
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
		RenameDialog.rename(mainWindow, pkg);
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

	private JMenuItem makeExportSubMenu(JPackage pkg) {
		JMenu exportSubMenu = new JMenu(NLS.str("popup.export"));

		exportSubMenu.add(makeExportMenuItem(pkg, NLS.str("tabs.code"), JClassExportType.Code));
		exportSubMenu.add(makeExportMenuItem(pkg, NLS.str("tabs.smali"), JClassExportType.Smali));
		exportSubMenu.add(makeExportMenuItem(pkg, "Simple", JClassExportType.Simple));
		exportSubMenu.add(makeExportMenuItem(pkg, "Fallback", JClassExportType.Fallback));

		return exportSubMenu;
	}

	public JMenuItem makeExportMenuItem(JPackage pkg, String label, JClassExportType exportType) {
		JMenuItem exportMenuItem = new JMenuItem(label);
		exportMenuItem.addActionListener(event -> {
			FileDialogWrapper fileDialog = new FileDialogWrapper(mainWindow, FileOpenMode.EXPORT_NODE_FOLDER);

			List<Path> selectedPaths = fileDialog.show();
			if (selectedPaths.size() != 1) {
				return;
			}

			Path savePath = selectedPaths.get(0);
			saveJPackage(pkg, savePath, exportType);
		});

		return exportMenuItem;
	}

	private static void saveJPackage(JPackage pkg, Path savePath, JClassExportType exportType) {
		Path subSavePath = savePath.resolve(pkg.getName());
		try {
			if (!Files.isDirectory(subSavePath)) {
				Files.createDirectory(subSavePath);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		for (JClass jClass : pkg.getClasses()) {
			String fileName = jClass.getName() + "." + exportType.extension;
			JClassPopupMenu.saveJClass(jClass, subSavePath.resolve(fileName), exportType);
		}
		for (JPackage subPkg : pkg.getSubPackages()) {
			saveJPackage(subPkg, subSavePath, exportType);
		}
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
