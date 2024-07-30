package jadx.gui.ui.popupmenu;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

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

		JNode jClassSimple = new JCodeMode(jClass, DecompilationMode.SIMPLE);
		JNode jClassFallback = new JCodeMode(jClass, DecompilationMode.FALLBACK);

		exportSubMenu.add(makeExportMenuItem(jClass, NLS.str("tabs.code"), "java",
				(cls) -> cls.getCodeInfo().getCodeStr()));
		exportSubMenu.add(makeExportMenuItem(jClass, NLS.str("tabs.smali"), "smali",
				JClass::getSmali));
		exportSubMenu.add(makeExportMenuItem(jClassSimple, "Simple", "java",
				(cls) -> cls.getCodeInfo().getCodeStr()));
		exportSubMenu.add(makeExportMenuItem(jClassFallback, "Fallback", "java",
				(cls) -> cls.getCodeInfo().getCodeStr()));

		return exportSubMenu;
	}

	public <T extends JNode> JMenuItem makeExportMenuItem(T jClass, String label, String extension, Function<T, String> getCode) {
		JMenuItem exportMenuItem = new JMenuItem(label);
		exportMenuItem.addActionListener(event -> {
			String fileName = jClass.getName() + "." + extension;

			FileDialogWrapper fileDialog = new FileDialogWrapper(mainWindow, FileOpenMode.EXPORT_NODE);
			fileDialog.setFileExtList(Collections.singletonList(extension));
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
			if (extension != null &&
					!selectedPath.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension)) {
				savePath = selectedPath.resolveSibling(selectedPath.getFileName() + "." + extension);
			} else {
				savePath = selectedPath;
			}

			try (Writer writer = Files.newBufferedWriter(savePath, StandardCharsets.UTF_8)) {
				writer.write(getCode.apply(jClass));
			} catch (Exception e) {
				throw new RuntimeException("Error saving project", e);
			}

			LOG.info("Done saving " + savePath);
		});

		return exportMenuItem;
	}
}
