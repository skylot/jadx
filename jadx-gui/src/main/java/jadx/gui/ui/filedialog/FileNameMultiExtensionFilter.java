package jadx.gui.ui.filedialog;

import java.io.File;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Custom file filter for filtering files with multiple extensions.
 * It overcomes the limitation of {@link FileNameExtensionFilter},
 * which treats only the last file extension split by dots as the
 * file extension, and does not support multiple extensions such as
 * {@code .jadx.kts}.
 */
class FileNameMultiExtensionFilter extends FileFilter {
	private final FileNameExtensionFilter delegate;
	private final String[] extensions;

	public FileNameMultiExtensionFilter(String description, String... extensions) {
		this.delegate = new FileNameExtensionFilter(description, extensions[0]);
		this.extensions = extensions;
	}

	@Override
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		}
		String fileName = file.getName();
		for (String extension : extensions) {
			if (fileName.endsWith(extension)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getDescription() {
		return delegate.getDescription();
	}
}
