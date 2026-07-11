package jadx.gui.ui.codearea;

import java.awt.BorderLayout;
import java.awt.Component;
import java.nio.charset.StandardCharsets;

import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ResourcesLoader;
import jadx.gui.jobs.BackgroundExecutor;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.hexviewer.HexPreviewPanel;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.utils.UiUtils;

public class BinaryContentPanel extends AbstractCodeContentPanel {
	private static final Logger LOG = LoggerFactory.getLogger(BinaryContentPanel.class);
	private final transient HexPreviewPanel hexPreviewPanel;

	public BinaryContentPanel(TabbedPane panel, JNode jnode) {
		super(panel, jnode);
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(0, 0, 0, 0));
		hexPreviewPanel = new HexPreviewPanel(getSettings());
		hexPreviewPanel.getInspector().setVisible(false);
		add(hexPreviewPanel, BorderLayout.CENTER);

		SwingUtilities.invokeLater(this::loadHexView);
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

	@Override
	public AbstractCodeArea getCodeArea() {
		return null;
	}

	@Override
	public void scrollToPos(int pos) {
		UiUtils.uiThreadGuard();
		BackgroundExecutor bgExec = getMainWindow().getBackgroundExecutor();
		bgExec.startLoading(this::loadHexView, () -> hexPreviewPanel.scrollToOffset(pos));
	}

	@Override
	public Component getChildrenComponent() {
		return hexPreviewPanel;
	}

	@Override
	public void loadSettings() {
		updateUI();
	}
}
