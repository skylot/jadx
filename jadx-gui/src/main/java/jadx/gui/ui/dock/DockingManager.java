package jadx.gui.ui.dock;

import java.awt.Component;
import java.io.File;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.andrewauclair.moderndocking.Dockable;
import io.github.andrewauclair.moderndocking.DockableTabPreference;
import io.github.andrewauclair.moderndocking.DockingRegion;
import io.github.andrewauclair.moderndocking.app.Docking;
import io.github.andrewauclair.moderndocking.app.DockingState;
import io.github.andrewauclair.moderndocking.app.LayoutPersistence;
import io.github.andrewauclair.moderndocking.app.RootDockingPanel;
import io.github.andrewauclair.moderndocking.event.DockingEvent;
import io.github.andrewauclair.moderndocking.event.DockingListener;
import io.github.andrewauclair.moderndocking.settings.Settings;

import jadx.api.plugins.events.IJadxEvents;
import jadx.api.plugins.events.types.DockableHidden;
import jadx.api.plugins.events.types.DockableShown;
import jadx.api.plugins.gui.DockableOptions;
import jadx.commons.app.JadxCommonFiles;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

/**
 * Owns the main window dockables: their default placement, show/hide, layout persistence and the
 * notification when the user closes a panel from its header.
 */
public class DockingManager {
	private static final Logger LOG = LoggerFactory.getLogger(DockingManager.class);
	private static final String LAYOUT_FILE = "gui-layout.xml";

	public enum Panel {
		TREE("tree", NLS.str("dock.tree"), false),
		EDITORS("editors", NLS.str("dock.editors"), false),
		QUICK_TABS("quick-tabs", NLS.str("dock.quick_tabs"), true),
		LOG("log", NLS.str("log_viewer.title"), true),
		DEBUGGER("debugger", NLS.str("dock.debugger"), true);

		final String id;
		final String title;
		final boolean closable;

		Panel(String id, String title, boolean closable) {
			this.id = id;
			this.title = title;
			this.closable = closable;
		}
	}

	private final JFrame frame;
	private final IJadxEvents events;
	private final Map<Panel, DockPanel> docks = new EnumMap<>(Panel.class);
	private final Map<Panel, Consumer<Panel>> closeListeners = new EnumMap<>(Panel.class);
	private final Map<String, PluginDock> pluginDocks = new LinkedHashMap<>();
	private final Set<String> shownPluginDocks = new HashSet<>();
	private final Map<String, Consumer<Boolean>> pluginDockListeners = new HashMap<>();
	private final DockingListener dockingListener = this::onDockingEvent;
	private @Nullable TabbedPane editors;
	private boolean layoutRestored;

	public DockingManager(JFrame frame, IJadxEvents events) {
		this.frame = frame;
		this.events = events;
		Docking.initialize(frame);
		Settings.setDefaultTabPreference(DockableTabPreference.TOP_ALWAYS);
		Settings.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		for (Panel panel : Panel.values()) {
			docks.put(panel, new DockPanel(panel.id, panel.title, panel.closable, new JPanel()).register());
		}
		docks.get(Panel.EDITORS).setHeadless(true);
		Docking.addDockingListener(dockingListener);
	}

	public RootDockingPanel createRootPanel() {
		return new RootDockingPanel(frame);
	}

	public DockPanel get(Panel panel) {
		return docks.get(panel);
	}

	public void setContent(Panel panel, Component content) {
		docks.get(panel).setContent(content);
	}

	public void setEditors(@Nullable TabbedPane editors) {
		this.editors = editors;
	}

	public void onClose(Panel panel, Consumer<Panel> listener) {
		closeListeners.put(panel, listener);
	}

	public boolean isShown(Panel panel) {
		return Docking.isDocked(docks.get(panel));
	}

	public void show(Panel panel) {
		DockPanel dock = docks.get(panel);
		if (!Docking.isDocked(dock)) {
			dockDefault(panel);
		}
		Docking.bringToFront(dock);
	}

	public void hide(Panel panel) {
		DockPanel dock = docks.get(panel);
		if (Docking.isDocked(dock)) {
			Docking.undock(dock);
		}
	}

	/**
	 * Register a plugin panel; shown on first run if requested, afterwards by the saved layout.
	 */
	public PluginDock registerPlugin(String persistentId, DockableOptions options, Supplier<JComponent> content) {
		PluginDock[] result = new PluginDock[1];
		UiUtils.uiRunAndWait(() -> result[0] = registerPluginOnUi(persistentId, options, content));
		return result[0];
	}

	private PluginDock registerPluginOnUi(String persistentId, DockableOptions options, Supplier<JComponent> content) {
		PluginDock dock = findRegistered(persistentId);
		if (dock != null) {
			shownPluginDocks.remove(persistentId);
			dock.bind(this, options, content);
			if (dock.isVisibleInLayout()) {
				onPluginDockEvent(dock, DockingEvent.ID.SHOWN);
			}
		} else {
			dock = new PluginDock(this, persistentId, options, content);
			dock.register();
		}
		pluginDocks.put(persistentId, dock);
		if (!dock.isDocked() && layoutRestored && options.isShowOnStartup() && !layoutFile().exists()) {
			show(dock);
		}
		return dock;
	}

	private @Nullable PluginDock findRegistered(String persistentId) {
		PluginDock known = pluginDocks.get(persistentId);
		if (known != null) {
			return known;
		}
		for (Dockable dockable : Docking.getDockables()) {
			if (dockable instanceof PluginDock && dockable.getPersistentID().equals(persistentId)) {
				return (PluginDock) dockable;
			}
		}
		return null;
	}

	public void onPluginDockChange(PluginDock dock, Consumer<Boolean> listener) {
		pluginDockListeners.put(dock.getPersistentID(), listener);
		listener.accept(dock.isDocked());
	}

	public void show(PluginDock dock) {
		dock.getContent();
		if (!dock.isDocked()) {
			dockDefault(dock);
		}
		Docking.bringToFront(dock);
	}

	public void hide(PluginDock dock) {
		if (dock.isDocked()) {
			Docking.undock(dock);
		}
	}

	public void remove(PluginDock dock) {
		UiUtils.uiRunAndWait(() -> {
			if (pluginDocks.get(dock.getPersistentID()) != dock) {
				return;
			}
			hide(dock);
			pluginDocks.remove(dock.getPersistentID());
			shownPluginDocks.remove(dock.getPersistentID());
			pluginDockListeners.remove(dock.getPersistentID());
			Docking.deregisterDockable(dock);
		});
	}

	private void dockDefault(PluginDock dock) {
		switch (dock.getOptions().getDefaultArea()) {
			case LEFT:
				Docking.dock(dock, frame, DockingRegion.WEST, 0.2);
				break;
			case RIGHT:
				Docking.dock(dock, frame, DockingRegion.EAST, 0.25);
				break;
			case BOTTOM:
				DockPanel log = docks.get(Panel.LOG);
				if (Docking.isDocked(log)) {
					Docking.dock(dock, log, DockingRegion.CENTER);
				} else {
					Docking.dock(dock, frame, DockingRegion.SOUTH, 0.3);
				}
				break;
			case EDITORS:
				if (editors != null) {
					editors.dockWithEditors(dock);
				} else {
					Docking.dock(dock, docks.get(Panel.EDITORS), DockingRegion.CENTER);
				}
				break;
		}
	}

	private void dockDefault(Panel panel) {
		DockPanel dock = docks.get(panel);
		switch (panel) {
			case TREE:
				Docking.dock(dock, frame, DockingRegion.WEST, 0.2);
				break;
			case EDITORS:
				Docking.dock(dock, docks.get(Panel.TREE), DockingRegion.EAST, 0.8);
				break;
			case QUICK_TABS:
				Docking.dock(dock, docks.get(Panel.TREE), DockingRegion.EAST, 0.4);
				break;
			case LOG:
				Docking.dock(dock, frame, DockingRegion.SOUTH, 0.3);
				break;
			case DEBUGGER:
				Docking.dock(dock, frame, DockingRegion.SOUTH, 0.35);
				break;
		}
	}

	/**
	 * Restore the persisted layout, or the default one; [shown] panels get docked afterwards.
	 */
	public void restoreLayout(Panel... shown) {
		File file = layoutFile();
		boolean firstRun = !file.exists();
		boolean restored = false;
		if (!firstRun) {
			try {
				DockingState.restoreApplicationLayout(LayoutPersistence.loadApplicationLayoutFromFile(file));
				restored = isShown(Panel.TREE) && isShown(Panel.EDITORS);
			} catch (Exception e) {
				LOG.warn("Failed to restore window layout, using default", e);
			}
		}
		if (!restored) {
			resetLayout();
		}
		for (Panel panel : new Panel[] { Panel.QUICK_TABS, Panel.LOG, Panel.DEBUGGER }) {
			boolean wanted = false;
			for (Panel s : shown) {
				wanted |= s == panel;
			}
			if (wanted) {
				show(panel);
			} else {
				hide(panel);
			}
		}
		layoutRestored = true;
		for (PluginDock dock : pluginDocks.values()) {
			if (firstRun && dock.getOptions().isShowOnStartup()) {
				show(dock);
			} else if (dock.isVisibleInLayout()) {
				onPluginDockEvent(dock, DockingEvent.ID.SHOWN);
			}
		}
	}

	public void resetLayout() {
		if (editors != null) {
			editors.undockAll();
		}
		for (Panel panel : Panel.values()) {
			hide(panel);
		}
		for (PluginDock dock : pluginDocks.values()) {
			hide(dock);
		}
		dockDefault(Panel.TREE);
		dockDefault(Panel.EDITORS);
		if (editors != null) {
			editors.dockAll();
		}
	}

	public void saveLayout() {
		try {
			if (editors != null) {
				editors.undockAll();
			}
			LayoutPersistence.saveLayoutToFile(layoutFile(), DockingState.getApplicationLayout());
		} catch (Exception e) {
			LOG.warn("Failed to save window layout", e);
		}
	}

	public void shutdown() {
		try {
			Docking.removeDockingListener(dockingListener);
			if (editors != null) {
				editors.undockAll();
			}
			Docking.deregisterAllDockables();
			Docking.deregisterAllDockingPanels();
		} catch (Exception e) {
			LOG.debug("Docking shutdown failed", e);
		}
	}

	private void onDockingEvent(DockingEvent event) {
		if (event.isTemporary()) {
			return;
		}
		Dockable dockable = event.getDockable();
		if (dockable instanceof PluginDock) {
			onPluginDockEvent((PluginDock) dockable, event.getID());
			return;
		}
		if (event.getID() != DockingEvent.ID.UNDOCKED) {
			return;
		}
		for (Map.Entry<Panel, DockPanel> e : docks.entrySet()) {
			if (e.getValue() == dockable) {
				Consumer<Panel> listener = closeListeners.get(e.getKey());
				if (listener != null) {
					listener.accept(e.getKey());
				}
				return;
			}
		}
	}

	private void onPluginDockEvent(PluginDock dock, DockingEvent.ID id) {
		String key = dock.getPersistentID();
		switch (id) {
			case DOCKED:
			case SHOWN:
				if (shownPluginDocks.add(key)) {
					dock.getContent();
					events.send(new DockableShown(dock.getId()));
				}
				notifyPluginDock(key, true);
				break;
			case UNDOCKED:
			case HIDDEN:
				if (shownPluginDocks.remove(key)) {
					events.send(new DockableHidden(dock.getId()));
				}
				notifyPluginDock(key, dock.isDocked());
				break;
			default:
				break;
		}
	}

	private void notifyPluginDock(String key, boolean docked) {
		Consumer<Boolean> listener = pluginDockListeners.get(key);
		if (listener != null) {
			listener.accept(docked);
		}
	}

	private static File layoutFile() {
		return JadxCommonFiles.getConfigDir().resolve(LAYOUT_FILE).toFile();
	}
}
