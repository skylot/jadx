package jadx.gui.ui.export;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.export.ExportGradle;
import jadx.core.export.ExportGradleType;
import jadx.core.utils.files.FileUtils;
import jadx.gui.JadxWrapper;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.dialog.CommonDialog;
import jadx.gui.ui.filedialog.FileDialogWrapper;
import jadx.gui.ui.filedialog.FileOpenMode;
import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;
import jadx.gui.utils.ui.DocumentUpdateListener;

public class ExportProjectDialog extends CommonDialog {
	private static final Logger LOG = LoggerFactory.getLogger(ExportProjectDialog.class);

	private final ExportProjectProperties exportProjectProperties = new ExportProjectProperties();
	private final Consumer<ExportProjectProperties> exportListener;

	public ExportProjectDialog(MainWindow mainWindow, Consumer<ExportProjectProperties> exportListener) {
		super(mainWindow);
		this.exportListener = exportListener;
		initUI();
	}

	private void initUI() {
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BorderLayout(5, 5));
		contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPanel.add(makeContentPane(), BorderLayout.PAGE_START);
		contentPanel.add(initButtonsPanel(), BorderLayout.PAGE_END);
		getContentPane().add(contentPanel);

		setTitle(NLS.str("export_dialog.title"));
		commonWindowInit();
	}

	private JPanel makeContentPane() {
		JLabel pathLbl = new JLabel(NLS.str("export_dialog.save_path"));
		JTextField pathField = new JTextField();
		pathField.getDocument().addDocumentListener(new DocumentUpdateListener(ev -> setExportProjectPath(pathField)));
		pathField.setText(mainWindow.getSettings().getLastSaveFilePath().toString());
		TextStandardActions.attach(pathField);

		JButton browseButton = makeEditorBrowseButton(pathField);

		JCheckBox resourceDecode = new JCheckBox(NLS.str("preferences.skipResourcesDecode"));
		resourceDecode.setSelected(mainWindow.getSettings().isSkipResources());
		resourceDecode.addItemListener(e -> {
			exportProjectProperties.setSkipResources(e.getStateChange() == ItemEvent.SELECTED);
		});

		JCheckBox skipSources = new JCheckBox(NLS.str("preferences.skipSourcesDecode"));
		skipSources.setSelected(mainWindow.getSettings().isSkipSources());
		skipSources.addItemListener(e -> {
			exportProjectProperties.setSkipSources(e.getStateChange() == ItemEvent.SELECTED);
		});

		JLabel exportTypeLbl = new JLabel(NLS.str("export_dialog.export_gradle_type"));
		JComboBox<ExportGradleType> exportTypeComboBox = new JComboBox<>(ExportGradleType.values());
		exportTypeLbl.setLabelFor(exportTypeComboBox);
		ExportGradleType initialExportType = getExportGradleType();
		exportProjectProperties.setExportGradleType(initialExportType);
		exportTypeComboBox.setSelectedItem(initialExportType);
		exportTypeComboBox.addItemListener(e -> {
			exportProjectProperties.setExportGradleType((ExportGradleType) e.getItem());
		});
		exportTypeComboBox.setEnabled(false);

		JCheckBox exportAsGradleProject = new JCheckBox(NLS.str("export_dialog.export_gradle"));
		exportAsGradleProject.addItemListener(e -> {
			boolean enableGradle = e.getStateChange() == ItemEvent.SELECTED;
			exportProjectProperties.setAsGradleMode(enableGradle);
			exportTypeComboBox.setEnabled(enableGradle);
			resourceDecode.setEnabled(!enableGradle);
			skipSources.setEnabled(!enableGradle);
		});

		JPanel pathPanel = new JPanel();
		pathPanel.setLayout(new BoxLayout(pathPanel, BoxLayout.LINE_AXIS));
		pathPanel.setAlignmentX(LEFT_ALIGNMENT);
		pathPanel.add(pathLbl);
		pathPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		pathPanel.add(pathField);
		pathPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		pathPanel.add(browseButton);

		JPanel typePanel = new JPanel();
		typePanel.setLayout(new BoxLayout(typePanel, BoxLayout.LINE_AXIS));
		typePanel.setAlignmentX(LEFT_ALIGNMENT);
		typePanel.add(Box.createRigidArea(new Dimension(20, 0)));
		typePanel.add(exportTypeLbl);
		typePanel.add(Box.createRigidArea(new Dimension(5, 0)));
		typePanel.add(exportTypeComboBox);
		typePanel.add(Box.createHorizontalGlue());

		JPanel exportOptionsPanel = new JPanel();
		exportOptionsPanel.setBorder(BorderFactory.createTitledBorder(NLS.str("export_dialog.export_options")));
		exportOptionsPanel.setLayout(new BoxLayout(exportOptionsPanel, BoxLayout.PAGE_AXIS));
		exportOptionsPanel.add(exportAsGradleProject);
		exportOptionsPanel.add(typePanel);
		exportOptionsPanel.add(resourceDecode);
		exportOptionsPanel.add(skipSources);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
		mainPanel.add(pathPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		mainPanel.add(exportOptionsPanel);
		return mainPanel;
	}

	private ExportGradleType getExportGradleType() {
		try {
			JadxWrapper wrapper = mainWindow.getWrapper();
			return ExportGradle.detectExportType(wrapper.getRootNode(), wrapper.getResources());
		} catch (Exception e) {
			LOG.warn("Failed to detect export type", e);
			return ExportGradleType.AUTO;
		}
	}

	private void setExportProjectPath(JTextField field) {
		String path = field.getText();
		if (!path.isEmpty()) {
			exportProjectProperties.setExportPath(field.getText());
		}
	}

	protected JPanel initButtonsPanel() {
		JButton cancelButton = new JButton(NLS.str("common_dialog.cancel"));
		cancelButton.addActionListener(event -> dispose());

		JButton exportProjectButton = new JButton(NLS.str("common_dialog.ok"));
		exportProjectButton.addActionListener(event -> exportProject());
		getRootPane().setDefaultButton(exportProjectButton);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(exportProjectButton);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(cancelButton);
		return buttonPane;
	}

	private JButton makeEditorBrowseButton(final JTextField textField) {
		JButton button = new JButton(NLS.str("export_dialog.browse"));
		button.addActionListener(e -> {
			FileDialogWrapper fileDialog = new FileDialogWrapper(mainWindow, FileOpenMode.EXPORT);
			mainWindow.getSettings().setLastSaveFilePath(fileDialog.getCurrentDir());
			List<Path> saveDirs = fileDialog.show();
			if (saveDirs.isEmpty()) {
				return;
			}
			String path = saveDirs.get(0).toString();
			textField.setText(path);
		});
		return button;
	}

	private void exportProject() {
		String exportPathStr = exportProjectProperties.getExportPath();
		if (!validateAndMakeDir(exportPathStr)) {
			JOptionPane.showMessageDialog(this, NLS.str("message.enter_valid_path"),
					NLS.str("message.errorTitle"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		mainWindow.getSettings().setLastSaveFilePath(Path.of(exportPathStr));
		LOG.debug("Export properties: {}", exportProjectProperties);
		exportListener.accept(exportProjectProperties);
		dispose();
	}

	private static boolean validateAndMakeDir(String exportPath) {
		if (exportPath == null || exportPath.isBlank()) {
			return false;
		}
		try {
			Path path = Path.of(exportPath);
			if (Files.isRegularFile(path)) {
				// dir exists as a file
				return false;
			}
			FileUtils.makeDirs(path);
			return true;
		} catch (Exception e) {
			LOG.warn("Export path validate error, path string:{}", exportPath, e);
			return false;
		}
	}
}
