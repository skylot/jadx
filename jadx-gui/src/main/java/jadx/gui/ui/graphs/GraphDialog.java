package jadx.gui.ui.graphs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.ListUtils;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.filedialog.FileDialogWrapper;
import jadx.gui.ui.filedialog.FileOpenMode;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.layout.WrapLayout;
import jadx.gui.utils.ui.MouseListenerAdapter;

public abstract class GraphDialog extends JFrame {
	private static final long serialVersionUID = 5840390965763493590L;

	private static final Logger LOG = LoggerFactory.getLogger(GraphDialog.class);
	private static final Dimension MIN_WINDOW_SIZE = new Dimension(800, 500);

	private final MainWindow mainWindow;
	private final GraphPanel panel;

	private JMenuBar menuBar = null;

	public static JTextArea graphError(String errorMessage) {
		JTextArea errorText = new JTextArea();
		errorText.setText(errorMessage);
		errorText.setVisible(true);
		errorText.setEditable(false);
		errorText.setLineWrap(false);
		return errorText;
	}

	public static JTextArea graphError(Exception error) {
		JTextArea errorText = new JTextArea();
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		stringWriter.write(NLS.str("graph_viewer.default_error"));
		stringWriter.write(": ");
		error.printStackTrace(printWriter);
		errorText.setText(stringWriter.toString());
		errorText.setVisible(true);
		errorText.setEditable(false);
		errorText.setLineWrap(false);
		return errorText;
	}

	protected GraphDialog(MainWindow mainWindow) {
		this(mainWindow, NLS.str("graph_viewer.default_title"));
	}

	public GraphDialog(MainWindow mainWindow, String title) {
		super(title);
		this.mainWindow = mainWindow;
		setMinimumSize(MIN_WINDOW_SIZE);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		UiUtils.addEscapeShortCutToDispose(this);
		setLocationRelativeTo(null);
		loadWindowPos();

		panel = new GraphPanel(this);
		panel.setFocusable(true);
		panel.addMouseListener(new MouseListenerAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				requestFocusInWindow();
			}
		});
		setLayout(new BorderLayout());
		add(panel, BorderLayout.CENTER);
	}

	public JMenuBar addMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		menuBar.setLayout(new WrapLayout(FlowLayout.LEFT));
		add(menuBar, BorderLayout.PAGE_START);
		this.menuBar = menuBar;

		JButton saveButton = new JButton(NLS.str("graph_viewer.save_graph"));
		saveButton.setEnabled(false);
		saveButton.addActionListener(e -> {
			try {
				FileDialogWrapper fileDialog = new FileDialogWrapper(mainWindow, FileOpenMode.CUSTOM_SAVE);
				fileDialog.setTitle(NLS.str("graph_viewer.save_graph"));
				fileDialog.setFileExtList(Collections.singletonList("svg"));
				fileDialog.setSelectionMode(JFileChooser.FILES_ONLY);
				List<Path> savePaths = fileDialog.show();
				if (!savePaths.isEmpty()) {
					File saveFile = ListUtils.first(savePaths).toFile();
					getPanel().exportSVG(saveFile);
				}
			} catch (Exception ex) {
				LOG.error("Failed to save file: ", ex);
				JOptionPane.showMessageDialog(this, NLS.str("graph_viewer.file_failure"),
						NLS.str("graph_viewer.file_failure"),
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
		JPanel menuBarPanel = new JPanel();
		menuBarPanel.setOpaque(false);
		menuBarPanel.add(saveButton);
		menuBar.add(menuBarPanel);
		return menuBar;
	}

	protected void enableMenu() {
		JMenuBar menu = this.menuBar;
		setAllEnabled(true, menu);
	}

	protected void disableMenu() {
		JMenuBar menu = this.menuBar;
		setAllEnabled(false, menu);
	}

	private void setAllEnabled(boolean isEnabled, JComponent component) {
		component.setEnabled(isEnabled);
		Component[] components = component.getComponents();
		for (Component subComponent : components) {
			if (subComponent instanceof JComponent) {
				setAllEnabled(isEnabled, (JComponent) subComponent);
			} else {
				subComponent.setEnabled(isEnabled);
			}
		}
	}

	public void loadWindowPos() {
		if (!mainWindow.getSettings().loadWindowPos(this)) {
			setPreferredSize(MIN_WINDOW_SIZE);
		}
	}

	@Override
	public void dispose() {
		try {
			mainWindow.getSettings().saveWindowPos(this);
		} catch (Exception e) {
			LOG.warn("Failed to save window size and position", e);
		}
		super.dispose();
	}

	protected GraphPanel getPanel() {
		return this.panel;
	}
}
