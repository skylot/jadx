package jadx.gui.ui.filedialog;

import java.awt.FileDialog;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import jadx.core.utils.ListUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.files.FileUtils;

class CustomFileDialog {

	private final FileDialogWrapper data;

	public CustomFileDialog(FileDialogWrapper data) {
		this.data = data;
	}

	public List<Path> showDialog() {
		FileDialog fileDialog = new FileDialog(data.getMainWindow(), data.getTitle());
		fileDialog.setMode(data.isOpen() ? FileDialog.LOAD : FileDialog.SAVE);
		fileDialog.setMultipleMode(true);
		List<String> fileExtList = data.getFileExtList();
		if (Utils.notEmpty(fileExtList)) {
			fileDialog.setFilenameFilter((dir, name) -> ListUtils.anyMatch(fileExtList, name::endsWith));
		}
		if (data.getSelectedFile() != null) {
			fileDialog.setFile(data.getSelectedFile().toAbsolutePath().toString());
		}
		if (data.getCurrentDir() != null) {
			fileDialog.setDirectory(data.getCurrentDir().toAbsolutePath().toString());
		}
		fileDialog.setVisible(true);
		File[] selectedFiles = fileDialog.getFiles();
		if (!Utils.isEmpty(selectedFiles)) {
			data.setCurrentDir(Paths.get(fileDialog.getDirectory()));
			return FileUtils.toPaths(selectedFiles);
		}
		if (fileDialog.getFile() != null) {
			return Collections.singletonList(Paths.get(fileDialog.getFile()));
		}
		return Collections.emptyList();
	}
}
