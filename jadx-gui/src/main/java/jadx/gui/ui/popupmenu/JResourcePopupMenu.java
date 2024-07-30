package jadx.gui.ui.popupmenu;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
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

import jadx.api.ResourcesLoader;
import jadx.api.plugins.utils.CommonFileUtils;
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.filedialog.FileDialogWrapper;
import jadx.gui.ui.filedialog.FileOpenMode;
import jadx.gui.utils.NLS;

public class JResourcePopupMenu extends JPopupMenu {
	private static final long serialVersionUID = -7781009781149260806L;

	private static final Logger LOG = LoggerFactory.getLogger(JResourcePopupMenu.class);

	private final transient MainWindow mainWindow;

	public JResourcePopupMenu(MainWindow mainWindow, JResource resource) {
		this.mainWindow = mainWindow;

		add(makeExportMenuItem(resource));
	}

	private JMenuItem makeExportMenuItem(JResource resource) {
		JMenuItem exportMenu = new JMenuItem(NLS.str("popup.export"));
		exportMenu.addActionListener(event -> {
			String extension = CommonFileUtils.getFileExtension(resource.getName());
			System.out.println(resource.getResFile().getType());

			FileDialogWrapper fileDialog = new FileDialogWrapper(mainWindow, FileOpenMode.EXPORT_NODE);
			fileDialog.setFileExtList(Collections.singletonList(extension));
			Path currentDir = fileDialog.getCurrentDir();
			if (currentDir != null) {
				fileDialog.setSelectedFile(currentDir.resolve(resource.getName()));
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

			switch (resource.getResFile().getType()) {
				case MANIFEST:
				case XML:
					exportString(savePath, resource);
					break;
				default:
					exportBinary(savePath, resource);
					break;
			}

			LOG.info("Done saving " + savePath);
		});
		return exportMenu;
	}

	private static void exportString(Path savePath, JResource resource) {
		try (Writer writer = Files.newBufferedWriter(savePath, StandardCharsets.UTF_8)) {
			writer.write(resource.getCodeInfo().getCodeStr());
		} catch (Exception e) {
			throw new RuntimeException("Error saving project", e);
		}
	}

	private static void exportBinary(Path savePath, JResource resource) {
		try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(savePath.toFile()))) {
			byte[] bytes = ResourcesLoader.decodeStream(resource.getResFile(), (size, is) -> is.readAllBytes());

			if (bytes == null) {
				bytes = new byte[0];
			}
			os.write(bytes);
		} catch (Exception e) {
			throw new RuntimeException("Error saving file " + resource.getName(), e);
		}
	}
}
