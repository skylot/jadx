package jadx.gui.utils.ui;

import java.awt.Desktop;
import java.awt.Frame;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ResourceFile;
import jadx.api.ResourcesLoader;
import jadx.core.plugins.files.TempFilesGetter;
import jadx.gui.treemodel.JResource;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

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
				UiUtils.errorMessage(frame, NLS.str("error_dialog.not_found_file", filePath));
				return;
			}
			if (Files.isDirectory(filePath)) {
				UiUtils.errorMessage(frame, NLS.str("error_dialog.path_is_directory", filePath));
				return;
			}
			if (!Files.isReadable(filePath)) {
				UiUtils.errorMessage(frame, NLS.str("error_dialog.cannot_read", filePath));
				return;
			}

			try {
				desktop.open(filePath.toFile());
			} catch (IOException ex) {
				UiUtils.errorMessage(frame, NLS.str("error_dialog.open_failed", ex.getMessage()));
				LOG.error("Unable to open file: {0}", ex);
			} catch (IllegalArgumentException ex) {
				UiUtils.errorMessage(frame, NLS.str("error_dialog.invalid_path_format", ex.getMessage()));
				LOG.error("Invalid file path: {0}", ex);
			}

		} else {
			UiUtils.errorMessage(frame, NLS.str("error_dialog.desktop_unsupported"));
		}
	}
}
