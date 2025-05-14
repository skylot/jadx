package jadx.gui.utils.ui;

import java.awt.Desktop;
import java.awt.Frame;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ResourceFile;
import jadx.api.ResourcesLoader;
import jadx.core.plugins.files.TempFilesGetter;
import jadx.gui.treemodel.JResource;
import jadx.gui.utils.NLS;

public class FileOpenerHelper {
	private static final Logger LOG = LoggerFactory.getLogger(FileOpenerHelper.class);

	public static void exportBinary(JResource resource, Path savePath) {
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

	public static void openFile(Frame frame, JResource res) {
		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			Path tempDir = TempFilesGetter.INSTANCE.getTempDir();
			ResourceFile resFile = res.getResFile();
			Path path = Paths.get(resFile.getDeobfName());
			Path fileNamePath = path.getFileName();
			Path filePath = tempDir.resolve(fileNamePath);
			exportBinary(res, filePath);

			if (!Files.exists(filePath)) {
				JOptionPane.showMessageDialog(frame,
						NLS.str("error_dialog.not_found_file", filePath),
						NLS.str("error_dialog.title"),
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (Files.isDirectory(filePath)) {
				JOptionPane.showMessageDialog(frame,
						NLS.str("error_dialog.path_is_directory", filePath),
						NLS.str("error_dialog.title"),
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (!Files.isReadable(filePath)) {
				JOptionPane.showMessageDialog(frame,
						NLS.str("error_dialog.cannot_read", filePath),
						NLS.str("error_dialog.title"),
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			try {
				desktop.open(filePath.toFile());
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(frame,
						NLS.str("error_dialog.open_failed", ex.getMessage()),
						NLS.str("error_dialog.title"),
						JOptionPane.ERROR_MESSAGE);
				LOG.error("Unable to open file: {0}", ex);
			} catch (IllegalArgumentException ex) {
				JOptionPane.showMessageDialog(frame,
						NLS.str("error_dialog.invalid_path_format", ex.getMessage()),
						NLS.str("error_dialog.title"),
						JOptionPane.ERROR_MESSAGE);
				LOG.error("Invalid file path: {0}", ex);
			}

		} else {
			JOptionPane.showMessageDialog(frame,
					NLS.str("error_dialog.desktop_unsupported"),
					NLS.str("error_dialog.title"),
					JOptionPane.ERROR_MESSAGE);
		}
	}
}
