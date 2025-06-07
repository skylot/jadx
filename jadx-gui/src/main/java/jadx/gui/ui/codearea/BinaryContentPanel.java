package jadx.gui.ui.codearea;

import java.awt.BorderLayout;
import java.awt.Component;
import java.nio.charset.StandardCharsets;

import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ResourcesLoader;
import jadx.api.resources.ResourceContentType;
import jadx.gui.jobs.BackgroundExecutor;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.LineNumbersMode;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.hexviewer.HexPreviewPanel;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.utils.UiUtils;

public class BinaryContentPanel extends AbstractCodeContentPanel {
	private static final Logger LOG = LoggerFactory.getLogger(BinaryContentPanel.class);
	private final transient CodePanel textCodePanel;
	private final transient HexPreviewPanel hexPreviewPanel;
	private final transient JTabbedPane areaTabbedPane;

	public BinaryContentPanel(TabbedPane panel, JNode jnode, boolean supportsText) {
		super(panel, jnode);
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(0, 0, 0, 0));
		if (supportsText) {
			textCodePanel = new CodePanel(new CodeArea(this, jnode));
		} else {
			textCodePanel = null;
		}
		hexPreviewPanel = new HexPreviewPanel(getSettings());
		hexPreviewPanel.getInspector().setVisible(false);

		areaTabbedPane = buildTabbedPane();
		add(areaTabbedPane);

		SwingUtilities.invokeLater(this::loadSelectedPanel);
	}

	private void loadHexView() {
		if (hexPreviewPanel.isDataLoaded()) {
			return;
		}
		UiUtils.notUiThreadGuard();
		byte[] bytes = getNodeBytes();
		UiUtils.uiRunAndWait(() -> hexPreviewPanel.setData(bytes));
	}

	private byte[] getNodeBytes() {
		JNode binaryNode = getNode();
		if (binaryNode instanceof JResource) {
			JResource jResource = (JResource) binaryNode;
			try {
				return ResourcesLoader.decodeStream(jResource.getResFile(), (size, is) -> is.readAllBytes());
			} catch (Exception e) {
				LOG.error("Failed to directly load resource binary data {}: {}", jResource.getName(), e.getMessage());
			}
		}
		return binaryNode.getCodeInfo().getCodeStr().getBytes(StandardCharsets.US_ASCII);
	}

	private JTabbedPane buildTabbedPane() {
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);
		tabbedPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		if (textCodePanel != null) {
			tabbedPane.add(textCodePanel, "Text");
		}
		tabbedPane.add(hexPreviewPanel, "Hex");
		tabbedPane.addChangeListener(e -> {
			getMainWindow().toggleHexViewMenu();
			loadSelectedPanel();
		});
		return tabbedPane;
	}

	private void loadSelectedPanel() {
		BackgroundExecutor bgExec = getMainWindow().getBackgroundExecutor();
		Component codePanel = getSelectedPanel();
		if (codePanel instanceof CodeArea) {
			CodeArea codeArea = (CodeArea) codePanel;
			bgExec.startLoading(codeArea::load);
		} else {
			bgExec.startLoading(this::loadHexView);
		}
	}

	@Override
	public AbstractCodeArea getCodeArea() {
		if (textCodePanel != null) {
			return textCodePanel.getCodeArea();
		} else {
			return null;
		}
	}

	@Override
	public void scrollToPos(int pos) {
		BackgroundExecutor bgExec = getMainWindow().getBackgroundExecutor();
		if (getNode().getContentType() == ResourceContentType.CONTENT_TEXT) {
			areaTabbedPane.setSelectedComponent(textCodePanel);
			AbstractCodeArea codeArea = textCodePanel.getCodeArea();
			bgExec.startLoading(codeArea::load, () -> codeArea.scrollToPos(pos));
		} else {
			areaTabbedPane.setSelectedComponent(hexPreviewPanel);
			bgExec.startLoading(this::loadHexView, () -> hexPreviewPanel.scrollToOffset(pos));
		}
	}

	@Override
	public Component getChildrenComponent() {
		return getSelectedPanel();
	}

	@Override
	public void loadSettings() {
		if (textCodePanel != null) {
			textCodePanel.loadSettings();
		}
		updateUI();
	}

	@Override
	public JadxSettings getSettings() {
		JadxSettings settings = super.getSettings();
		settings.setLineNumbersMode(LineNumbersMode.NORMAL);
		return settings;
	}

	private Component getSelectedPanel() {
		Component selectedComponent = areaTabbedPane.getSelectedComponent();
		Component selectedPanel;
		if (selectedComponent instanceof CodePanel) {
			selectedPanel = ((CodePanel) selectedComponent).getCodeArea();
		} else if (selectedComponent instanceof JSplitPane) {
			selectedPanel = ((JSplitPane) selectedComponent).getLeftComponent();
		} else if (selectedComponent instanceof HexPreviewPanel) {
			selectedPanel = selectedComponent;
		} else {
			throw new RuntimeException("tabbedPane.getSelectedComponent returned a Component "
					+ "of unexpected type " + selectedComponent);
		}
		return selectedPanel;
	}
}
