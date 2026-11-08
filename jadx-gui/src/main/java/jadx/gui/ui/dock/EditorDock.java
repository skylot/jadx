package jadx.gui.ui.dock;

import java.awt.Component;
import java.awt.Container;
import java.util.function.Consumer;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.jetbrains.annotations.Nullable;

import jadx.api.gui.code.JadxContentPanel;

public class EditorDock extends DockPanel {
	private final JadxContentPanel panel;
	private @Nullable String tabText;
	private @Nullable Icon tabIcon;
	private boolean closable = true;
	private @Nullable Consumer<JPopupMenu> menuBuilder;
	private @Nullable Runnable closeRequest;

	public EditorDock(String id, JadxContentPanel panel) {
		super(id, "", true, panel);
		this.panel = panel;
		register();
	}

	public JadxContentPanel getPanel() {
		return panel;
	}

	public void setMenuBuilder(@Nullable Consumer<JPopupMenu> menuBuilder) {
		this.menuBuilder = menuBuilder;
	}

	public void setCloseRequest(@Nullable Runnable closeRequest) {
		this.closeRequest = closeRequest;
	}

	public void update(String text, @Nullable Icon icon, boolean canClose) {
		this.tabText = text;
		this.tabIcon = icon;
		this.closable = canClose;
		JTabbedPane tabs = enclosingTabs();
		if (tabs == null) {
			return;
		}
		Container display = getParent();
		int index = display == null ? -1 : tabs.indexOfComponent(display);
		if (index < 0) {
			return;
		}
		tabs.setTitleAt(index, text);
		tabs.setIconAt(index, icon);
		tabs.setToolTipTextAt(index, panel.getNode().getTooltip());
		if (display instanceof javax.swing.JComponent) {
			((javax.swing.JComponent) display).putClientProperty("JTabbedPane.tabClosable", canClose);
		}
	}

	public @Nullable JTabbedPane enclosingTabs() {
		Component c = getParent();
		while (c != null && !(c instanceof JTabbedPane)) {
			c = c.getParent();
		}
		return (JTabbedPane) c;
	}

	@Override
	public String getTabText() {
		String text = tabText != null ? tabText : panel.getNode().getName();
		if (text == null) {
			return "?";
		}
		return text;
	}

	@Override
	public String getTabTooltip() {
		String tooltip = panel.getNode().getTooltip();
		return tooltip == null ? "" : tooltip;
	}

	@Override
	public @Nullable Icon getIcon() {
		return tabIcon != null ? tabIcon : panel.getNode().getIcon();
	}

	@Override
	public boolean isClosable() {
		return true;
	}

	@Override
	public boolean requestClose() {
		if (closeRequest != null) {
			closeRequest.run();
			return false;
		}
		return closable;
	}

	@Override
	public boolean hasMoreMenuOptions() {
		return menuBuilder != null;
	}

	@Override
	public void addMoreOptions(JPopupMenu menu) {
		if (menuBuilder != null) {
			menuBuilder.accept(menu);
		}
	}

	public void showMenu(Component invoker, int x, int y) {
		if (menuBuilder == null) {
			return;
		}
		JPopupMenu menu = new JPopupMenu();
		menuBuilder.accept(menu);
		SwingUtilities.updateComponentTreeUI(menu);
		menu.show(invoker, x, y);
	}
}
