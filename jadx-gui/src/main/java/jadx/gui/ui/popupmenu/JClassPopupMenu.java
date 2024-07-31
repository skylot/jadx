package jadx.gui.ui.popupmenu;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.DecompilationMode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.mode.JCodeMode;
import jadx.gui.ui.dialog.RenameDialog;
import jadx.gui.ui.filedialog.FileDialogWrapper;
import jadx.gui.ui.filedialog.FileOpenMode;
import jadx.gui.utils.NLS;

public class JClassPopupMenu extends JPopupMenu {
	private static final long serialVersionUID = -7781009781149260806L;

	private static final Logger LOG = LoggerFactory.getLogger(JClassPopupMenu.class);

	private final transient MainWindow mainWindow;

	public JClassPopupMenu(MainWindow mainWindow, JClass jClass) {
		this.mainWindow = mainWindow;

		add(RenameDialog.buildRenamePopupMenuItem(mainWindow, jClass));
		add(makeExportSubMenu(jClass));
	}

	private JMenuItem makeExportSubMenu(JClass jClass) {
		JMenu exportSubMenu = new JMenu(NLS.str("popup.export"));

		exportSubMenu.add(makeExportMenuItem(jClass, NLS.str("tabs.code"), JClassExportType.Code));
		exportSubMenu.add(makeExportMenuItem(jClass, NLS.str("tabs.smali"), JClassExportType.Smali));
		exportSubMenu.add(makeExportMenuItem(jClass, "Simple", JClassExportType.Simple));
		exportSubMenu.add(makeExportMenuItem(jClass, "Fallback", JClassExportType.Fallback));

		return exportSubMenu;
	}

	public JMenuItem makeExportMenuItem(JClass jClass, String label, JClassExportType exportType) {
		JMenuItem exportMenuItem = new JMenuItem(label);
		exportMenuItem.addActionListener(event -> {
			String fileName = jClass.getName() + "." + exportType.extension;

			FileDialogWrapper fileDialog = new FileDialogWrapper(mainWindow, FileOpenMode.EXPORT_NODE);
			fileDialog.setFileExtList(Collections.singletonList(exportType.extension));
			Path currentDir = fileDialog.getCurrentDir();
			if (currentDir != null) {
				fileDialog.setSelectedFile(currentDir.resolve(fileName));
			}

			List<Path> selectedPaths = fileDialog.show();
			if (selectedPaths.size() != 1) {
				return;
			}

			Path selectedPath = selectedPaths.get(0);
			Path savePath;
			// Append file extension if missing
			if (!selectedPath.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(exportType.extension)) {
				savePath = selectedPath.resolveSibling(selectedPath.getFileName() + "." + exportType.extension);
			} else {
				savePath = selectedPath;
			}

			saveJClass(jClass, savePath, exportType);

			LOG.info("Done saving {}", savePath);
		});

		return exportMenuItem;
	}

	public static void saveJClass(JClass jClass, Path savePath, JClassExportType exportType) {
		try (Writer writer = Files.newBufferedWriter(savePath, StandardCharsets.UTF_8)) {
			writer.write(getCode(jClass, exportType));
		} catch (Exception e) {
			throw new RuntimeException("Error saving project", e);
		}
	}

	private static String getCode(JClass jClass, JClassExportType exportType) {
		switch (exportType) {
			case Code:
				return jClass.getCodeInfo().getCodeStr();
			case Smali:
				return jClass.getSmali();
			case Simple:
				JNode jClassSimple = new JCodeMode(jClass, DecompilationMode.SIMPLE);
				return jClassSimple.getCodeInfo().getCodeStr();
			case Fallback:
				JNode jClassFallback = new JCodeMode(jClass, DecompilationMode.FALLBACK);
				return jClassFallback.getCodeInfo().getCodeStr();
			default:
				throw new RuntimeException("Unsupported JClassExportType " + exportType);
		}
	}
}
