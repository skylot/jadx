package jadx.gui.ui.filedialog;

import java.awt.Component;
import java.awt.Container;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

import jadx.api.plugins.utils.CommonFileUtils;
import jadx.core.utils.StringUtils;
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
			List<String> validFileExtList = fileExtList.stream()
					.filter(StringUtils::notBlank)
					.collect(Collectors.toList());
			if (Utils.notEmpty(validFileExtList)) {
				String description = NLS.str("file_dialog.supported_files") + ": (" + Utils.listToString(validFileExtList) + ')';
				setFileFilter(new FileNameMultiExtensionFilter(description, validFileExtList.toArray(new String[0])));
			}
		}
		if (data.getSelectedFile() != null) {
			setSelectedFile(data.getSelectedFile().toFile());
		}
		if (data.isOpen()) {
			installFileListPasteAction(this);
		}
		MainWindow mainWindow = data.getMainWindow();
		int ret = data.isOpen() ? showOpenDialog(mainWindow) : showSaveDialog(mainWindow);
		if (ret != JFileChooser.APPROVE_OPTION) {
			return Collections.emptyList();
		}
		data.setCurrentDir(getCurrentDirectory().toPath());
		File[] selectedFiles = getSelectedFiles();
		if (selectedFiles.length != 0) {
			return FileUtils.toPathsWithTrim(selectedFiles);
		}
		File chosenFile = getSelectedFile();
		if (chosenFile != null) {
			return Collections.singletonList(FileUtils.toPathWithTrim(chosenFile));
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

	private void installFileListPasteAction(Component component) {
		if (component instanceof JTextComponent) {
			JTextComponent textComponent = (JTextComponent) component;
			Action defaultPasteAction = textComponent.getActionMap().get(DefaultEditorKit.pasteAction);
			textComponent.getActionMap().put(DefaultEditorKit.pasteAction, new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (!pasteFileListFromClipboard(textComponent)) {
						if (defaultPasteAction != null) {
							defaultPasteAction.actionPerformed(e);
						} else {
							textComponent.paste();
						}
					}
				}
			});
		}
		if (component instanceof Container) {
			for (Component child : ((Container) component).getComponents()) {
				installFileListPasteAction(child);
			}
		}
	}

	private boolean pasteFileListFromClipboard(JTextComponent textComponent) {
		try {
			Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
			if (contents == null || !contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				return false;
			}
			@SuppressWarnings("unchecked")
			List<File> clipboardFiles = (List<File>) contents.getTransferData(DataFlavor.javaFileListFlavor);
			String paths = clipboardFiles.stream()
					.filter(Objects::nonNull)
					.map(file -> '"' + file.getAbsolutePath() + '"')
					.collect(Collectors.joining(" "));
			if (paths.isEmpty()) {
				return false;
			}
			textComponent.replaceSelection(paths);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
