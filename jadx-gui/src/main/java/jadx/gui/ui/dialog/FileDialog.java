package jadx.gui.ui.dialog;

import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jetbrains.annotations.Nullable;

import jadx.core.utils.Utils;
import jadx.gui.settings.JadxProject;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.FileUtils;
import jadx.gui.utils.NLS;

public class FileDialog {

	public enum OpenMode {
		OPEN, ADD, SAVE_PROJECT, EXPORT
	}

	private final MainWindow mainWindow;

	private boolean isOpen;
	private String title;
	private List<String> fileExtList;
	private int selectionMode = JFileChooser.FILES_AND_DIRECTORIES;
	private @Nullable Path currentDir;
	private @Nullable Path selectedFile;

	public FileDialog(MainWindow mainWindow, OpenMode mode) {
		this.mainWindow = mainWindow;
		initForMode(mode);
	}

	public List<Path> show() {
		FileChooser fileChooser = buildFileChooser();
		int ret = isOpen ? fileChooser.showOpenDialog(mainWindow) : fileChooser.showSaveDialog(mainWindow);
		if (ret != JFileChooser.APPROVE_OPTION) {
			return Collections.emptyList();
		}
		currentDir = fileChooser.getCurrentDirectory().toPath();
		return FileUtils.toPaths(fileChooser.getSelectedFiles());
	}

	public Path getCurrentDir() {
		return currentDir;
	}

	public void setSelectedFile(Path path) {
		this.selectedFile = path;
	}

	private void initForMode(OpenMode mode) {
		switch (mode) {
			case OPEN:
			case ADD:
				fileExtList = new ArrayList<>(Arrays.asList("apk", "dex", "jar", "class", "smali", "zip", "aar", "arsc"));
				if (mode == OpenMode.OPEN) {
					fileExtList.addAll(Arrays.asList(JadxProject.PROJECT_EXTENSION, "aab"));
					title = NLS.str("file.open_title");
				} else {
					title = NLS.str("file.add_files_action");
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
		}
	}

	private FileChooser buildFileChooser() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setToolTipText(title);
		fileChooser.setFileSelectionMode(selectionMode);
		fileChooser.setMultiSelectionEnabled(isOpen);
		fileChooser.setAcceptAllFileFilterUsed(true);
		if (!fileExtList.isEmpty()) {
			String description = NLS.str("file_dialog.supported_files") + ": (" + Utils.listToString(fileExtList) + ')';
			fileChooser.setFileFilter(new FileNameExtensionFilter(description, fileExtList.toArray(new String[0])));
		}
		if (currentDir != null) {
			fileChooser.setCurrentDirectory(currentDir.toFile());
		}
		if (selectedFile != null) {
			fileChooser.setSelectedFile(selectedFile.toFile());
		}
		return fileChooser;
	}

	private class FileChooser extends JFileChooser {
		@Override
		protected JDialog createDialog(Component parent) throws HeadlessException {
			JDialog dialog = super.createDialog(parent);
			dialog.setTitle(title);
			dialog.setLocationRelativeTo(null);
			mainWindow.getSettings().loadWindowPos(dialog);
			dialog.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent e) {
					mainWindow.getSettings().saveWindowPos(dialog);
					super.windowClosed(e);
				}
			});
			return dialog;
		}

		@Override
		public void approveSelection() {
			if (selectionMode == FILES_AND_DIRECTORIES) {
				File currentFile = getSelectedFile();
				if (currentFile.isDirectory()) {
					int option = JOptionPane.showConfirmDialog(
							mainWindow,
							NLS.str("file_dialog.load_dir_confirm") + "\n " + currentFile,
							NLS.str("file_dialog.load_dir_title"),
							JOptionPane.YES_NO_OPTION);
					if (option != JOptionPane.YES_OPTION) {
						this.setCurrentDirectory(currentFile);
						this.updateUI();
						return;
					}
				}
			}
			super.approveSelection();
		}
	}
}
