package jadx.gui.ui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.gui.treemodel.ApkSignature;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.codearea.AbstractCodeContentPanel;
import jadx.gui.ui.codearea.ClassCodeContentPanel;
import jadx.gui.ui.codearea.CodeContentPanel;
import jadx.gui.utils.JumpManager;
import jadx.gui.utils.JumpPosition;

public class TabbedPane extends JTabbedPane {

	private static final Logger LOG = LoggerFactory.getLogger(TabbedPane.class);
	private static final long serialVersionUID = -8833600618794570904L;

	private final transient MainWindow mainWindow;
	private final transient Map<JNode, ContentPanel> openTabs = new LinkedHashMap<>();
	private final transient JumpManager jumps = new JumpManager();

	TabbedPane(MainWindow window) {
		this.mainWindow = window;

		setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

		addMouseWheelListener(e -> {
			if (openTabs.isEmpty()) {
				return;
			}
			int direction = e.getWheelRotation();
			int index = getSelectedIndex();
			int maxIndex = getTabCount() - 1;
			if ((index == 0 && direction < 0)
					|| (index == maxIndex && direction > 0)) {
				index = maxIndex - index;
			} else {
				index += direction;
			}
			setSelectedIndex(index);
		});
	}

	public MainWindow getMainWindow() {
		return mainWindow;
	}

	private void showCode(final JumpPosition pos) {
		final AbstractCodeContentPanel contentPanel = (AbstractCodeContentPanel) getContentPanel(pos.getNode());
		if (contentPanel == null) {
			return;
		}
		SwingUtilities.invokeLater(() -> {
			setSelectedComponent(contentPanel);
			AbstractCodeArea codeArea = contentPanel.getCodeArea();
			int line = pos.getLine();
			if (line < 0) {
				try {
					line = 1 + codeArea.getLineOfOffset(-line);
				} catch (BadLocationException e) {
					LOG.error("Can't get line for: {}", pos, e);
					line = pos.getNode().getLine();
				}
			}
			codeArea.scrollToLine(line);
			codeArea.requestFocus();
		});
	}

	public void showResource(JResource res) {
		final ContentPanel contentPanel = getContentPanel(res);
		if (contentPanel == null) {
			return;
		}
		SwingUtilities.invokeLater(() -> setSelectedComponent(contentPanel));
	}

	public void showSimpleNode(JNode node) {
		final ContentPanel contentPanel = getContentPanel(node);
		if (contentPanel == null) {
			return;
		}
		SwingUtilities.invokeLater(() -> setSelectedComponent(contentPanel));
	}

	public void codeJump(JumpPosition pos) {
		JumpPosition curPos = getCurrentPosition();
		if (curPos != null) {
			jumps.addPosition(curPos);
			jumps.addPosition(pos);
		}
		showCode(pos);
	}

	@Nullable
	JumpPosition getCurrentPosition() {
		ContentPanel selectedCodePanel = getSelectedCodePanel();
		if (selectedCodePanel instanceof AbstractCodeContentPanel) {
			return ((AbstractCodeContentPanel) selectedCodePanel).getCodeArea().getCurrentPosition();
		}
		return null;
	}

	public void navBack() {
		JumpPosition pos = jumps.getPrev();
		if (pos != null) {
			showCode(pos);
		}
	}

	public void navForward() {
		JumpPosition pos = jumps.getNext();
		if (pos != null) {
			showCode(pos);
		}
	}

	private void addContentPanel(ContentPanel contentPanel) {
		openTabs.put(contentPanel.getNode(), contentPanel);
		add(contentPanel);
	}

	public void closeCodePanel(ContentPanel contentPanel) {
		openTabs.remove(contentPanel.getNode());
		remove(contentPanel);
	}

	@Nullable
	private ContentPanel getContentPanel(JNode node) {
		ContentPanel panel = openTabs.get(node);
		if (panel == null) {
			panel = makeContentPanel(node);
			if (panel == null) {
				return null;
			}
			addContentPanel(panel);
			setTabComponentAt(indexOfComponent(panel), makeTabComponent(panel));
		}
		return panel;
	}

	@Nullable
	private ContentPanel makeContentPanel(JNode node) {
		if (node instanceof JResource) {
			JResource res = (JResource) node;
			ResourceFile resFile = res.getResFile();
			if (resFile != null) {
				if (resFile.getType() == ResourceType.IMG) {
					return new ImagePanel(this, res);
				}
				return new CodeContentPanel(this, node);
			} else {
				return null;
			}
		}
		if (node instanceof ApkSignature) {
			return new HtmlPanel(this, node);
		}
		return new ClassCodeContentPanel(this, node);
	}

	@Nullable
	ContentPanel getSelectedCodePanel() {
		return (ContentPanel) getSelectedComponent();
	}

	private Component makeTabComponent(final ContentPanel contentPanel) {
		return new TabComponent(this, contentPanel);
	}

	public void closeAllTabs() {
		List<ContentPanel> contentPanels = new ArrayList<>(openTabs.values());
		for (ContentPanel panel : contentPanels) {
			closeCodePanel(panel);
		}
	}

	public Map<JNode, ContentPanel> getOpenTabs() {
		return openTabs;
	}

	public void loadSettings() {
		for (ContentPanel panel : openTabs.values()) {
			panel.loadSettings();
		}
		int tabCount = getTabCount();
		for (int i = 0; i < tabCount; i++) {
			Component tabComponent = getTabComponentAt(i);
			if (tabComponent instanceof TabComponent) {
				((TabComponent) tabComponent).loadSettings();
			}
		}
	}
}
