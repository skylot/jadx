package jadx.gui.ui.filedialog;

import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import jadx.api.plugins.utils.CommonFileUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.files.FileUtils;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;

class CustomFileChooser extends JFileChooser {

	static {
		// disable left shortcut panel, can crush in "Win32ShellFolderManager2.getNetwork()" or similar call
		UIManager.put("FileChooser.noPlacesBar", Boolean.TRUE);
	}

	private final FileDialogWrapper data;

	public CustomFileChooser(FileDialogWrapper data) {
		super(data.getCurrentDir() == null ? CommonFileUtils.CWD : data.getCurrentDir().toFile());
		putClientProperty("FileChooser.useShellFolder", Boolean.FALSE);
		this.data = data;
	}

	public List<Path> showDialog() {
		setToolTipText(data.getTitle());
		setFileSelectionMode(data.getSelectionMode());
		setMultiSelectionEnabled(data.isOpen());
		setAcceptAllFileFilterUsed(true);
		List<String> fileExtList = data.getFileExtList();
		if (Utils.notEmpty(fileExtList)) {
			String description = NLS.str("file_dialog.supported_files") + ": (" + Utils.listToString(fileExtList) + ')';
			setFileFilter(new FileNameExtensionFilter(description, fileExtList.toArray(new String[0])));
		}
		if (data.getSelectedFile() != null) {
			setSelectedFile(data.getSelectedFile().toFile());
		}
		MainWindow mainWindow = data.getMainWindow();
		int ret = data.isOpen() ? showOpenDialog(mainWindow) : showSaveDialog(mainWindow);
		if (ret != JFileChooser.APPROVE_OPTION) {
			return Collections.emptyList();
		}
		data.setCurrentDir(getCurrentDirectory().toPath());
		File[] selectedFiles = getSelectedFiles();
		if (selectedFiles.length != 0) {
			return FileUtils.toPaths(selectedFiles);
		}
		File chosenFile = getSelectedFile();
		if (chosenFile != null) {
			return Collections.singletonList(chosenFile.toPath());
		}
		return Collections.emptyList();
	}

	@Override
	protected JDialog createDialog(Component parent) throws HeadlessException {
		JDialog dialog = super.createDialog(parent);
		dialog.setTitle(data.getTitle());
		dialog.setLocationRelativeTo(null);
		data.getMainWindow().getSettings().loadWindowPos(dialog);
		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				data.getMainWindow().getSettings().saveWindowPos(dialog);
				super.windowClosed(e);
			}
		});
		return dialog;
	}

	@Override
	public void approveSelection() {
		if (data.getSelectionMode() == FILES_AND_DIRECTORIES) {
			File currentFile = getSelectedFile();
			if (currentFile.isDirectory()) {
				int option = JOptionPane.showConfirmDialog(
						data.getMainWindow(),
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
