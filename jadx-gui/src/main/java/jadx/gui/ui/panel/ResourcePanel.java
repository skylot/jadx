package jadx.gui.ui.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;

import org.jetbrains.annotations.Nullable;

import jadx.api.ResourceType;
import jadx.api.resources.ResourceContentType;
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.codearea.AbstractCodeContentPanel;
import jadx.gui.ui.codearea.BinaryContentPanel;
import jadx.gui.ui.codearea.CodeContentPanel;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.utils.NLS;

public class ResourcePanel extends AbstractCodeContentPanel {
	private static final long serialVersionUID = 340828212293293032L;

	private final transient JTabbedPane tabbedPane;

	/**
	 * Store the tab references that have been loaded so far.
	 * As we only want to track refernces we can use IdentityHashSet which does not make calls
	 * to hashCode() and equals() which make sit faster for large objects.
	 */
	private final Set<Component> initializedTabs = Collections.newSetFromMap(new IdentityHashMap<>());

	public ResourcePanel(TabbedPane panel, JResource resource) {
		super(panel, resource);
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(0, 0, 0, 0));

		tabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);
		tabbedPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		buildTabs(panel, resource);
		ChangeListener changeListener = e -> {
			Component selected = tabbedPane.getSelectedComponent();
			if (initializedTabs.add(selected)) {
				if (selected instanceof ILazyLoad) {
					ILazyLoad lazyLoadComponent = (ILazyLoad) selected;
					SwingUtilities.invokeLater(lazyLoadComponent::loadData);
				}
			}
			getMainWindow().updateHexViewMenuEnabled();
		};
		tabbedPane.addChangeListener(changeListener);
		add(tabbedPane, BorderLayout.CENTER);
		changeListener.stateChanged(null);
	}

	private void buildTabs(TabbedPane panel, JResource resource) {
		ResourceType resourceType = resource.getResFile().getType();
		switch (resourceType) {
			case IMG:
				tabbedPane.addTab(NLS.str("tabs.image"), new ImagePanel(panel, resource));
				tabbedPane.addTab(NLS.str("tabs.hex"), new BinaryContentPanel(panel, resource));
				return;

			case FONT:
				tabbedPane.addTab(NLS.str("tabs.font"), new FontPanel(panel, resource));
				tabbedPane.addTab(NLS.str("tabs.hex"), new BinaryContentPanel(panel, resource));
				return;
		}

		if (resource.getContentType() == ResourceContentType.CONTENT_BINARY) {
			tabbedPane.addTab(NLS.str("tabs.hex"), new BinaryContentPanel(panel, resource));
			return;
		}

		if (resource.hasSyntaxByExtension()) {
			tabbedPane.addTab(NLS.str("tabs.code"), new CodeContentPanel(panel, resource));
			return;
		}

		// unknown file type, show both text and binary
		tabbedPane.addTab(NLS.str("tabs.code"), new CodeContentPanel(panel, resource));
		tabbedPane.addTab(NLS.str("tabs.hex"), new BinaryContentPanel(panel, resource));
	}

	@Override
	public @Nullable AbstractCodeArea getCodeArea() {
		AbstractCodeContentPanel selectedCodePanel = getSelectedCodePanel();
		if (selectedCodePanel != null) {
			return selectedCodePanel.getCodeArea();
		}
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			Component component = tabbedPane.getComponentAt(i);
			if (component instanceof AbstractCodeContentPanel) {
				return ((AbstractCodeContentPanel) component).getCodeArea();
			}
		}
		return null;
	}

	@Override
	public void scrollToPos(int pos) {
		AbstractCodeContentPanel selectedCodePanel = getSelectedCodePanel();
		if (selectedCodePanel != null) {
			selectedCodePanel.scrollToPos(pos);
			return;
		}
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			Component component = tabbedPane.getComponentAt(i);
			if (component instanceof AbstractCodeContentPanel) {
				tabbedPane.setSelectedIndex(i);
				((AbstractCodeContentPanel) component).scrollToPos(pos);
				return;
			}
		}
	}

	@Override
	public Component getChildrenComponent() {
		Component selectedComponent = tabbedPane.getSelectedComponent();
		if (selectedComponent instanceof AbstractCodeContentPanel) {
			return ((AbstractCodeContentPanel) selectedComponent).getChildrenComponent();
		}
		return selectedComponent;
	}

	@Override
	public void loadSettings() {
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			Component component = tabbedPane.getComponentAt(i);
			if (component instanceof ContentPanel) {
				((ContentPanel) component).loadSettings();
			}
		}
		updateUI();
	}

	@Override
	public void dispose() {
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			Component component = tabbedPane.getComponentAt(i);
			if (component instanceof ContentPanel) {
				((ContentPanel) component).dispose();
			}
		}
		super.dispose();
	}

	private @Nullable AbstractCodeContentPanel getSelectedCodePanel() {
		Component selectedComponent = tabbedPane.getSelectedComponent();
		if (selectedComponent instanceof AbstractCodeContentPanel) {
			return (AbstractCodeContentPanel) selectedComponent;
		}
		return null;
	}
}
