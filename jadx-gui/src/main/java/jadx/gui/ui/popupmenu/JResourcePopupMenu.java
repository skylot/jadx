package jadx.gui.ui.popupmenu;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.utils.CommonFileUtils;
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.filedialog.FileDialogWrapper;
import jadx.gui.ui.filedialog.FileOpenMode;
import jadx.gui.utils.NLS;
import jadx.gui.utils.ui.FileOpenerHelper;

public class JResourcePopupMenu extends JPopupMenu {
	private static final long serialVersionUID = -7781009781149260806L;

	private static final Logger LOG = LoggerFactory.getLogger(JResourcePopupMenu.class);

	private final transient MainWindow mainWindow;

	public JResourcePopupMenu(MainWindow mainWindow, JResource resource) {
		this.mainWindow = mainWindow;

		if (resource.getType() != JResource.JResType.ROOT) {
			add(makeExportMenuItem(resource));
		}
	}

	private JMenuItem makeExportMenuItem(JResource resource) {
		JMenuItem exportMenu = new JMenuItem(NLS.str("popup.export"));
		exportMenu.addActionListener(event -> {
			Path savePath = null;
			switch (resource.getType()) {
				case ROOT:
				case DIR:
					savePath = getSaveDirPath(resource);
					break;
				case FILE:
					savePath = getSaveFilePath(resource);
					break;
			}

			if (savePath == null) {
				return;
			}

			saveJResource(resource, savePath, true);

			LOG.info("Done saving {}", savePath);
		});
		return exportMenu;
	}

	private Path getSaveFilePath(JResource resource) {
		String extension = CommonFileUtils.getFileExtension(resource.getName());

		FileDialogWrapper fileDialog = new FileDialogWrapper(mainWindow, FileOpenMode.EXPORT_NODE);
		fileDialog.setFileExtList(Collections.singletonList(extension));
		Path currentDir = fileDialog.getCurrentDir();
		if (currentDir != null) {
			fileDialog.setSelectedFile(currentDir.resolve(resource.getName()));
		}

		List<Path> selectedPaths = fileDialog.show();
		if (selectedPaths.size() != 1) {
			return null;
		}

		Path selectedPath = selectedPaths.get(0);
		Path savePath;
		// Append file extension if missing
		if (extension != null
				&& !selectedPath.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension)) {
			savePath = selectedPath.resolveSibling(selectedPath.getFileName() + "." + extension);
		} else {
			savePath = selectedPath;
		}

		return savePath;
	}

	private Path getSaveDirPath(JResource resource) {
		FileDialogWrapper fileDialog = new FileDialogWrapper(mainWindow, FileOpenMode.EXPORT_NODE_FOLDER);

		List<Path> selectedPaths = fileDialog.show();
		if (selectedPaths.size() != 1) {
			return null;
		}

		return selectedPaths.get(0);
	}

	private static void saveJResource(JResource resource, Path savePath, boolean comingFromDialog) {
		switch (resource.getType()) {
			case ROOT:
			case DIR:
				saveJResourceDir(resource, savePath, comingFromDialog);
				break;
			case FILE:
				saveJResourceFile(resource, savePath, comingFromDialog);
				break;
		}
	}

	private static void saveJResourceDir(JResource resource, Path savePath, boolean comingFromDialog) {
		Path subSavePath = savePath.resolve(resource.getName());
		try {
			if (!Files.isDirectory(subSavePath)) {
				Files.createDirectory(subSavePath);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		for (JResource subResource : resource.getSubNodes()) {
			saveJResource(subResource, subSavePath, false);
		}
	}

	private static void saveJResourceFile(JResource resource, Path savePath, boolean comingFromDialog) {
		if (!comingFromDialog) {
			Path fileName = Path.of(resource.getName()).getFileName();
			savePath = savePath.resolve(fileName);
		}
		switch (resource.getResFile().getType()) {
			case MANIFEST:
			case XML:
				exportString(resource, savePath);
				break;
			default:
				FileOpenerHelper.exportBinary(resource, savePath);
				break;
		}
	}

	private static void exportString(JResource resource, Path savePath) {
		try (Writer writer = Files.newBufferedWriter(savePath, StandardCharsets.UTF_8)) {
			writer.write(resource.getCodeInfo().getCodeStr());
		} catch (Exception e) {
			throw new RuntimeException("Error saving file " + resource.getName(), e);
		}
	}

}
