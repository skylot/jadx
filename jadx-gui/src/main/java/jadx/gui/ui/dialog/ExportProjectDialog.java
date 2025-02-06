package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jadx.gui.settings.ExportProjectProperties;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.filedialog.FileDialogWrapper;
import jadx.gui.ui.filedialog.FileOpenMode;
import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;
import jadx.gui.utils.ui.DocumentUpdateListener;

public class ExportProjectDialog extends CommonDialog {

	private final ExportProjectProperties exportProjectProperties = new ExportProjectProperties();
	private final Consumer<ExportProjectProperties> exportListener;

	public ExportProjectDialog(MainWindow mainWindow, Consumer<ExportProjectProperties> exportListener) {
		super(mainWindow);
		this.exportListener = exportListener;
		initUI();
	}

	private void initUI() {
		JPanel contentPane = makeContentPane();
		JPanel buttonPane = initButtonsPanel();
		Container container = getContentPane();
		container.add(contentPane, BorderLayout.CENTER);
		container.add(buttonPane, BorderLayout.PAGE_END);

		setTitle(NLS.str("export_dialog.title"));
		commonWindowInit();
	}

	private JPanel makeContentPane() {
		// top layout
		JLabel label = new JLabel(NLS.str("export_dialog.save_path"));
		JTextField pathField = new JTextField();
		pathField.setText(mainWindow.getSettings().getLastSaveFilePath().toString());
		pathField.getDocument().addDocumentListener(new DocumentUpdateListener(ev -> setExportProjectPath(pathField)));
		TextStandardActions.attach(pathField);

		JButton browseButton = makeEditorBrowseButton(pathField);

		// check box layout
		JPanel exportOptionsPanel = new JPanel();
		exportOptionsPanel.setBorder(BorderFactory.createTitledBorder(NLS.str("export_dialog.export_options")));
		exportOptionsPanel.setLayout(new BoxLayout(exportOptionsPanel, BoxLayout.PAGE_AXIS));

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

		JCheckBox exportAsGradleProject = new JCheckBox(NLS.str("export_dialog.export_gradle"));
		exportAsGradleProject.addItemListener(e -> {
			boolean isSelected = e.getStateChange() == ItemEvent.SELECTED;

			exportProjectProperties.setAsGradleMode(isSelected);
			resourceDecode.setEnabled(!isSelected);
			skipSources.setEnabled(!isSelected);
		});

		exportOptionsPanel.add(exportAsGradleProject);
		exportOptionsPanel.add(resourceDecode);
		exportOptionsPanel.add(skipSources);

		// build group box layout
		JPanel groupBoxPanel = new JPanel();
		GroupLayout groupBoxLayout = new GroupLayout(groupBoxPanel);
		groupBoxLayout.setAutoCreateGaps(true);
		groupBoxLayout.setAutoCreateContainerGaps(true);
		groupBoxPanel.setLayout(groupBoxLayout);

		groupBoxLayout.setHorizontalGroup(groupBoxLayout.createParallelGroup()
				.addComponent(exportOptionsPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Integer.MAX_VALUE));
		groupBoxLayout.setVerticalGroup(groupBoxLayout.createSequentialGroup()
				.addComponent(exportOptionsPanel));

		// main layout
		JPanel mainPanel = new JPanel();
		GroupLayout layout = new GroupLayout(mainPanel);
		mainPanel.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		// arrange components using GroupLayout
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
						.addComponent(label)
						.addComponent(pathField)
						.addComponent(browseButton))
				.addComponent(groupBoxPanel));

		layout.setVerticalGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(label)
						.addComponent(pathField)
						.addComponent(browseButton))
				.addComponent(groupBoxPanel));
		return mainPanel;
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
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
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
		if (!new File(exportProjectProperties.getExportPath()).exists()) {
			JOptionPane.showMessageDialog(this, NLS.str("message.enter_valid_path"),
					NLS.str("message.errorTitle"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		exportListener.accept(exportProjectProperties);
		dispose();
	}
}
