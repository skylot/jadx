package jadx.gui.ui.filedialog;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JFileChooser;

import org.jetbrains.annotations.Nullable;

import jadx.gui.settings.JadxProject;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;

public class FileDialogWrapper {

	private final MainWindow mainWindow;

	private boolean isOpen;
	private String title;
	private List<String> fileExtList;
	private int selectionMode = JFileChooser.FILES_AND_DIRECTORIES;
	private @Nullable Path currentDir;
	private @Nullable Path selectedFile;

	public FileDialogWrapper(MainWindow mainWindow, FileOpenMode mode) {
		this.mainWindow = mainWindow;
		initForMode(mode);
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setFileExtList(List<String> fileExtList) {
		this.fileExtList = fileExtList;
	}

	public void setSelectionMode(int selectionMode) {
		this.selectionMode = selectionMode;
	}

	public void setSelectedFile(Path path) {
		this.selectedFile = path;
	}

	public void setCurrentDir(Path currentDir) {
		this.currentDir = currentDir;
	}

	public List<Path> show() {
		if (mainWindow.getSettings().isUseAlternativeFileDialog()) {
			return new CustomFileDialog(this).showDialog();
		} else {
			return new CustomFileChooser(this).showDialog();
		}
	}

	private void initForMode(FileOpenMode mode) {
		switch (mode) {
			case OPEN:
			case OPEN_PROJECT:
			case ADD:
				if (mode == FileOpenMode.OPEN_PROJECT) {
					fileExtList = Collections.singletonList(JadxProject.PROJECT_EXTENSION);
					title = NLS.str("file.open_title");
				} else {
					fileExtList = new ArrayList<>(Arrays.asList("apk", "dex", "jar", "class", "smali", "zip", "xapk", "aar", "arsc"));
					if (mode == FileOpenMode.OPEN) {
						fileExtList.addAll(Arrays.asList(JadxProject.PROJECT_EXTENSION, "aab"));
						title = NLS.str("file.open_title");
					} else {
						title = NLS.str("file.add_files_action");
					}
				}
				selectionMode = JFileChooser.FILES_AND_DIRECTORIES;
				currentDir = mainWindow.getSettings().getLastOpenFilePath();
				isOpen = true;
				break;

			case SAVE_PROJECT:
				title = NLS.str("file.save_project");
				fileExtList = Collections.singletonList(JadxProject.PROJECT_EXTENSION);
				selectionMode = JFileChooser.FILES_ONLY;
				currentDir = mainWindow.getSettings().getLastSaveFilePath();
				isOpen = false;
				break;

			case EXPORT:
				title = NLS.str("file.save_all_msg");
				fileExtList = Collections.emptyList();
				selectionMode = JFileChooser.DIRECTORIES_ONLY;
				currentDir = mainWindow.getSettings().getLastSaveFilePath();
				isOpen = false;
				break;

			case CUSTOM_SAVE:
				isOpen = false;
				currentDir = mainWindow.getSettings().getLastSaveFilePath();
				break;

			case CUSTOM_OPEN:
				isOpen = true;
				currentDir = mainWindow.getSettings().getLastOpenFilePath();
				break;
		}
	}

	public Path getCurrentDir() {
		return currentDir;
	}

	public MainWindow getMainWindow() {
		return mainWindow;
	}

	public boolean isOpen() {
		return isOpen;
	}

	public String getTitle() {
		return title;
	}

	public List<String> getFileExtList() {
		return fileExtList;
	}

	public int getSelectionMode() {
		return selectionMode;
	}

	public Path getSelectedFile() {
		return selectedFile;
	}
}
