package jadx.gui.settings.ui.plugins;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatClientProperties;

import jadx.gui.ui.MainWindow;
import jadx.gui.ui.filedialog.FileDialogWrapper;
import jadx.gui.ui.filedialog.FileOpenMode;
import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;
import jadx.gui.utils.UiUtils;

public class InstallPluginDialog extends JDialog {
	private static final Logger LOG = LoggerFactory.getLogger(InstallPluginDialog.class);
	private static final long serialVersionUID = 5304314264730563853L;

	private final MainWindow mainWindow;
	private final PluginSettings pluginsSettings;
	private JTextField locationFld;

	public InstallPluginDialog(MainWindow mainWindow, PluginSettings pluginsSettings) {
		super(mainWindow, NLS.str("preferences.plugins.install"));
		this.mainWindow = mainWindow;
		this.pluginsSettings = pluginsSettings;
		init();
	}

	private void init() {
		locationFld = new JTextField();
		locationFld.setAlignmentX(LEFT_ALIGNMENT);
		locationFld.setColumns(50);
		TextStandardActions.attach(locationFld);
		locationFld.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);

		JLabel locationLbl = new JLabel(NLS.str("preferences.plugins.location_id_label"));
		locationLbl.setLabelFor(locationFld);

		JPanel locationPanel = new JPanel();
		locationPanel.setLayout(new BoxLayout(locationPanel, BoxLayout.LINE_AXIS));
		locationPanel.add(locationLbl);
		locationPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		locationPanel.add(locationFld);

		JButton fileBtn = new JButton(NLS.str("preferences.plugins.plugin_jar"));
		fileBtn.addActionListener(ev -> openPluginJar());
		JLabel fileLbl = new JLabel(NLS.str("preferences.plugins.plugin_jar_label"));
		fileLbl.setLabelFor(fileBtn);

		JPanel filePanel = new JPanel();
		filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.LINE_AXIS));
		filePanel.add(fileLbl);
		filePanel.add(Box.createRigidArea(new Dimension(5, 0)));
		filePanel.add(fileBtn);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.add(locationPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		mainPanel.add(filePanel);

		JButton installBtn = new JButton(NLS.str("preferences.plugins.install_btn"));
		installBtn.addActionListener(ev -> install());
		JButton cancelBtn = new JButton(NLS.str("preferences.cancel"));
		cancelBtn.addActionListener(ev -> dispose());

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		// TODO: add operation progress
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(installBtn);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(cancelBtn);
		getRootPane().setDefaultButton(installBtn);

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BorderLayout(5, 5));
		contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPanel.add(mainPanel, BorderLayout.PAGE_START);
		contentPanel.add(buttonPane, BorderLayout.PAGE_END);
		getContentPane().add(contentPanel);

		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.APPLICATION_MODAL);
		UiUtils.addEscapeShortCutToDispose(this);
	}

	private void openPluginJar() {
		FileDialogWrapper fd = new FileDialogWrapper(mainWindow, FileOpenMode.CUSTOM_OPEN);
		fd.setTitle(NLS.str("preferences.plugins.plugin_jar"));
		fd.setFileExtList(Collections.singletonList("jar"));
		fd.setSelectionMode(JFileChooser.FILES_ONLY);
		List<Path> files = fd.show();
		if (files.size() == 1) {
			locationFld.setText("file:" + files.get(0).toAbsolutePath());
		}
	}

	private void install() {
		pluginsSettings.install(locationFld.getText());
		dispose();
	}
}
