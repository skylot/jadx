package jadx.gui.ui.dock;

import java.awt.Component;
import java.util.function.Supplier;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.jetbrains.annotations.Nullable;

import io.github.andrewauclair.moderndocking.DynamicDockableParameters;
import io.github.andrewauclair.moderndocking.app.Docking;

import jadx.api.plugins.gui.DockableOptions;
import jadx.api.plugins.gui.JadxDockable;

public class PluginDock extends DockPanel implements JadxDockable {
	private @Nullable DockingManager manager;
	private DockableOptions options;
	private Supplier<JComponent> contentSupplier;
	private String title;
	private boolean closable;
	private @Nullable JComponent content;

	public PluginDock(DockingManager manager, String persistentId, DockableOptions options, Supplier<JComponent> contentSupplier) {
		super(persistentId, options.getTitle(), options.isClosable(), new JPanel());
		this.closable = options.isClosable();
		bind(manager, options, contentSupplier);
	}

	/**
	 * Placeholder created on layout restore for a plugin panel not registered yet
	 */
	public PluginDock(DynamicDockableParameters params) {
		super(params.getPersistentID(), params.getTabText(), true, new JPanel());
		this.closable = true;
		this.options = new DockableOptions("placeholder", params.getTabText().isEmpty() ? " " : params.getTabText());
		this.contentSupplier = JPanel::new;
		this.title = options.getTitle();
		register();
	}

	/**
	 * Attach the plugin panel to this placeholder
	 */
	void bind(DockingManager manager, DockableOptions options, Supplier<JComponent> contentSupplier) {
		this.manager = manager;
		this.options = options;
		this.contentSupplier = contentSupplier;
		this.title = options.getTitle();
		this.closable = options.isClosable();
		this.content = null;
		super.setIcon(options.getIcon());
		refreshTab();
	}

	public boolean isPlaceholder() {
		return manager == null;
	}

	public DockableOptions getOptions() {
		return options;
	}

	@Override
	public String getId() {
		return options.getId();
	}

	@Override
	public void show() {
		if (manager != null) {
			manager.show(this);
		}
	}

	@Override
	public void hide() {
		if (manager != null) {
			manager.hide(this);
		}
	}

	@Override
	public boolean isShown() {
		return isDocked();
	}

	boolean isDocked() {
		return Docking.isDockableRegistered(getPersistentID()) && Docking.isDocked(this);
	}

	/**
	 * Docked and not hidden behind another tab
	 */
	boolean isVisibleInLayout() {
		if (!isDocked()) {
			return false;
		}
		Component c = getParent();
		Component child = this;
		while (c != null && !(c instanceof JTabbedPane)) {
			child = c;
			c = c.getParent();
		}
		if (c == null) {
			return true;
		}
		JTabbedPane tabs = (JTabbedPane) c;
		return tabs.getSelectedComponent() == child;
	}

	@Override
	public void setTitle(String title) {
		this.title = title;
		refreshTab();
	}

	@Override
	public void setIcon(@Nullable ImageIcon icon) {
		super.setIcon(icon);
		refreshTab();
	}

	private void refreshTab() {
		if (isDocked()) {
			Docking.updateTabInfo(this);
		}
	}

	@Override
	public JComponent getContent() {
		if (content == null) {
			content = contentSupplier.get();
			setContent(content);
		}
		return content;
	}

	@Override
	public void remove() {
		if (manager != null) {
			manager.remove(this);
		}
	}

	@Override
	public String getTabText() {
		return title;
	}

	@Override
	public boolean isClosable() {
		return closable;
	}
}
