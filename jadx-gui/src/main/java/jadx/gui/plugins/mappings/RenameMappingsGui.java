package jadx.gui.plugins.mappings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.format.MappingFormat;

import jadx.api.args.UserRenamesMappingsMode;
import jadx.api.plugins.utils.CommonFileUtils;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.Utils;
import jadx.gui.jobs.TaskStatus;
import jadx.gui.settings.JadxProject;
import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JRoot;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.TabbedPane;
import jadx.gui.ui.filedialog.FileDialogWrapper;
import jadx.gui.ui.filedialog.FileOpenMode;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.ActionHandler;
import jadx.plugins.mappings.RenameMappingsOptions;
import jadx.plugins.mappings.save.MappingExporter;

public class RenameMappingsGui {
	private static final Logger LOG = LoggerFactory.getLogger(RenameMappingsGui.class);

	private final MainWindow mainWindow;

	private boolean renamesChanged = false;
	private JInputMapping mappingNode;

	private transient JMenu openMappingsMenu;
	private transient Action saveMappingsAction;
	private transient JMenu saveMappingsAsMenu;
	private transient Action closeMappingsAction;

	public RenameMappingsGui(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		mainWindow.addLoadListener(this::onLoad);
		mainWindow.addTreeUpdateListener(this::treeUpdate);
	}

	public void addMenuActions(JMenu menu) {
		openMappingsMenu = new JMenu(NLS.str("file.open_mappings"));
		openMappingsMenu.add(new ActionHandler(ev -> openMappings(MappingFormat.PROGUARD, true)).withNameAndDesc("Proguard (inverted)"));
		openMappingsMenu.add(new ActionHandler(ev -> openMappings(MappingFormat.PROGUARD, false)).withNameAndDesc("Proguard"));

		saveMappingsAction = new ActionHandler(this::saveMappings).withNameAndDesc(NLS.str("file.save_mappings"));

		saveMappingsAsMenu = new JMenu(NLS.str("file.save_mappings_as"));

		for (MappingFormat mappingFormat : MappingFormat.values()) {
			if (mappingFormat != MappingFormat.PROGUARD) {
				openMappingsMenu.add(new ActionHandler(ev -> openMappings(mappingFormat, false))
						.withNameAndDesc(mappingFormat.name));
			}
			saveMappingsAsMenu.add(new ActionHandler(ev -> saveMappingsAs(mappingFormat))
					.withNameAndDesc(mappingFormat.name));
		}

		closeMappingsAction = new ActionHandler(ev -> closeMappingsAndRemoveFromProject())
				.withNameAndDesc(NLS.str("file.close_mappings"));

		menu.addSeparator();
		menu.add(openMappingsMenu);
		menu.add(saveMappingsAction);
		menu.add(saveMappingsAsMenu);
		menu.add(closeMappingsAction);
	}

	private boolean onLoad(boolean loaded) {
		renamesChanged = false;
		mappingNode = null;
		if (loaded) {
			RootNode rootNode = mainWindow.getWrapper().getRootNode();
			rootNode.registerCodeDataUpdateListener(codeData -> onRename());
		} else {
			// project or window close
			JadxProject project = mainWindow.getProject();
			JadxSettings settings = mainWindow.getSettings();
			if (project.getMappingsPath() != null
					&& settings.getUserRenamesMappingsMode() == UserRenamesMappingsMode.READ_AND_AUTOSAVE_BEFORE_CLOSING) {
				saveMappings();
			}
		}
		return false;
	}

	private void onRename() {
		JadxProject project = mainWindow.getProject();
		JadxSettings settings = mainWindow.getSettings();
		if (project.getMappingsPath() != null
				&& settings.getUserRenamesMappingsMode() == UserRenamesMappingsMode.READ_AND_AUTOSAVE_EVERY_CHANGE) {
			saveMappings();
		} else {
			renamesChanged = true;
			UiUtils.uiRun(mainWindow::update);
		}
	}

	public void onUpdate(boolean loaded) {
		JadxProject project = mainWindow.getProject();
		openMappingsMenu.setEnabled(loaded);
		saveMappingsAction.setEnabled(loaded && renamesChanged && project.getMappingsPath() != null);
		saveMappingsAsMenu.setEnabled(loaded);
		closeMappingsAction.setEnabled(project.getMappingsPath() != null);
	}

	private void treeUpdate(JRoot treeRoot) {
		if (mappingNode != null) {
			// already added
			return;
		}
		Path mappingsPath = mainWindow.getProject().getMappingsPath();
		if (mappingsPath == null) {
			return;
		}
		JNode node = treeRoot.followStaticPath("JInputs");
		JNode currentNode = node.removeNode(n -> n.getClass().equals(JInputMapping.class));
		if (currentNode != null) {
			// close opened tab
			TabbedPane tabbedPane = mainWindow.getTabbedPane();
			ContentPanel openedTab = tabbedPane.getOpenTabs().get(currentNode);
			if (openedTab != null) {
				tabbedPane.closeCodePanel(openedTab);
			}
		}
		mappingNode = new JInputMapping(mappingsPath);
		node.add(mappingNode);
	}

	private void openMappings(MappingFormat mappingFormat, boolean inverted) {
		FileDialogWrapper fileDialog = new FileDialogWrapper(mainWindow, FileOpenMode.CUSTOM_OPEN);
		fileDialog.setTitle(NLS.str("file.open_mappings"));
		if (mappingFormat.hasSingleFile()) {
			fileDialog.setFileExtList(Collections.singletonList(mappingFormat.fileExt));
			fileDialog.setSelectionMode(JFileChooser.FILES_ONLY);
		} else {
			fileDialog.setSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		}
		List<Path> selectedPaths = fileDialog.show();
		if (selectedPaths.size() != 1) {
			return;
		}
		Path filePath = selectedPaths.get(0);
		LOG.info("Loading mappings from: {}", filePath.toAbsolutePath());
		JadxProject project = mainWindow.getProject();
		project.setMappingsPath(filePath);
		project.updatePluginOptions(options -> {
			options.put(RenameMappingsOptions.FORMAT_OPT, mappingFormat.name());
			options.put(RenameMappingsOptions.INVERT_OPT, inverted ? "yes" : "no");
		});
		mainWindow.reopen();
	}

	public void closeMappingsAndRemoveFromProject() {
		mainWindow.getProject().setMappingsPath(null);
		mainWindow.reopen();
	}

	private void saveMappings() {
		renamesChanged = false;
		saveInBackground(getCurrentMappingFormat(),
				mainWindow.getProject().getMappingsPath(),
				s -> mainWindow.update());
	}

	private void saveMappingsAs(MappingFormat mappingFormat) {
		FileDialogWrapper fileDialog = new FileDialogWrapper(mainWindow, FileOpenMode.CUSTOM_SAVE);
		fileDialog.setTitle(NLS.str("file.save_mappings_as"));
		if (mappingFormat.hasSingleFile()) {
			Path currentDir = Utils.getOrElse(fileDialog.getCurrentDir(), CommonFileUtils.CWD_PATH);
			fileDialog.setSelectedFile(currentDir.resolve("mappings." + mappingFormat.fileExt));
			fileDialog.setFileExtList(Collections.singletonList(mappingFormat.fileExt));
			fileDialog.setSelectionMode(JFileChooser.FILES_ONLY);
		} else {
			fileDialog.setSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		}
		List<Path> selectedPaths = fileDialog.show();
		if (selectedPaths.size() != 1) {
			return;
		}
		Path selectedPath = selectedPaths.get(0);
		Path savePath;
		// Append file extension if missing
		if (mappingFormat.hasSingleFile()
				&& !selectedPath.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(mappingFormat.fileExt)) {
			savePath = selectedPath.resolveSibling(selectedPath.getFileName() + "." + mappingFormat.fileExt);
		} else {
			savePath = selectedPath;
		}
		// If the target file already exists (and it's not an empty directory), show an overwrite
		// confirmation
		if (Files.exists(savePath)) {
			boolean emptyDir = false;
			try (Stream<Path> entries = Files.list(savePath)) {
				emptyDir = entries.findFirst().isEmpty();
			} catch (IOException ignored) {
			}
			if (!emptyDir) {
				int res = JOptionPane.showConfirmDialog(
						mainWindow,
						NLS.str("confirm.save_as_message", savePath.getFileName()),
						NLS.str("confirm.save_as_title"),
						JOptionPane.YES_NO_OPTION);
				if (res == JOptionPane.NO_OPTION) {
					return;
				}
			}
		}
		LOG.info("Saving mappings to: {}", savePath.toAbsolutePath());
		JadxProject project = mainWindow.getProject();
		project.setMappingsPath(savePath);
		project.updatePluginOptions(options -> {
			options.put(RenameMappingsOptions.FORMAT_OPT, mappingFormat.name());
			options.put(RenameMappingsOptions.INVERT_OPT, "no");
		});
		saveInBackground(mappingFormat, savePath, s -> {
			mappingNode = null;
			mainWindow.reloadTree();
		});
	}

	private void saveInBackground(MappingFormat mappingFormat, Path savePath, Consumer<TaskStatus> onFinishUiRunnable) {
		mainWindow.getBackgroundExecutor().execute(NLS.str("progress.save_mappings"),
				() -> new MappingExporter(mainWindow.getWrapper().getRootNode())
						.exportMappings(savePath, mainWindow.getProject().getCodeData(), mappingFormat),
				onFinishUiRunnable);
	}

	private MappingFormat getCurrentMappingFormat() {
		JadxProject project = mainWindow.getProject();
		String fmtStr = project.getPluginOption(RenameMappingsOptions.FORMAT_OPT);
		if (fmtStr != null) {
			return MappingFormat.valueOf(fmtStr);
		}
		Path mappingsPath = project.getMappingsPath();
		try {
			return MappingReader.detectFormat(mappingsPath);
		} catch (IOException e) {
			throw new RuntimeException("Failed to detect mapping format for: " + mappingsPath);
		}
	}
}
