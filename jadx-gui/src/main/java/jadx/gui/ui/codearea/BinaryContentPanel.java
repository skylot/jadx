package jadx.gui.ui.codearea;

import java.awt.BorderLayout;
import java.awt.Component;
import java.nio.charset.StandardCharsets;

import javax.swing.border.EmptyBorder;

import org.exbin.auxiliary.binary_data.BinaryData;
import org.exbin.auxiliary.binary_data.array.ByteArrayData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.jobs.BackgroundExecutor;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.hexviewer.HexPreviewPanel;
import jadx.gui.ui.hexviewer.LazyLoadingBinaryData;
import jadx.gui.ui.panel.ILazyLoad;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.utils.UiUtils;
import jadx.zip.IZipEntry;

public class BinaryContentPanel extends AbstractCodeContentPanel implements ILazyLoad {
	private static final Logger LOG = LoggerFactory.getLogger(BinaryContentPanel.class);
	private final transient HexPreviewPanel hexPreviewPanel;

	public BinaryContentPanel(TabbedPane panel, JNode jnode) {
		super(panel, jnode);
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(0, 0, 0, 0));
		hexPreviewPanel = new HexPreviewPanel(getSettings());
		hexPreviewPanel.getInspector().setVisible(false);
		add(hexPreviewPanel, BorderLayout.CENTER);
	}

	@Override
	public void loadData() {
		loadHexView();
	}

	private void loadHexView() {
		if (hexPreviewPanel.isDataLoaded()) {
			return;
		}
		LOG.debug("Loading Hex View of {}", node.getName());
		UiUtils.uiRunAndWait(() -> hexPreviewPanel.setData(getNodeData()));
	}

	private BinaryData getNodeData() {
		JNode binaryNode = getNode();
		if (binaryNode instanceof JResource) {
			JResource jResource = (JResource) binaryNode;
			try {
				IZipEntry zipEntry = jResource.getResFile().getZipEntry();
				if (zipEntry != null) {
					// we need an InputStream that will not be closed, therefore we can't use
					// ResourcesLoader.decodeStream
					return new LazyLoadingBinaryData(zipEntry.getInputStream(), zipEntry.getUncompressedSize());
				}
			} catch (Exception e) {
				LOG.error("Failed to directly load resource binary data {}: {}", jResource.getName(), e.getMessage());
			}
		}
		return new ByteArrayData(binaryNode.getCodeInfo().getCodeStr().getBytes(StandardCharsets.US_ASCII));
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

	@Override
	public void dispose() {
		hexPreviewPanel.dispose();
		super.dispose();
	}
}
