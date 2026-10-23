package jadx.api.gui.dock;

import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import org.jetbrains.annotations.Nullable;

import jadx.api.gui.plugins.JadxGuiContextExt;

/**
 * Dockable panel options for {@link JadxGuiContextExt#addDockable}
 */
public final class DockableOptions {
	private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9][a-z0-9-]*");

	private final String id;
	private final String title;
	private @Nullable ImageIcon icon;
	private DockArea defaultArea = DockArea.BOTTOM;
	private boolean closable = true;
	private boolean showOnStartup = false;
	private boolean menuEntry = true;
	private @Nullable String keyBinding;

	/**
	 * @param id    panel id, unique inside plugin
	 * @param title panel tab title
	 */
	public DockableOptions(String id, String title) {
		if (!ID_PATTERN.matcher(id).matches()) {
			throw new IllegalArgumentException("Dockable id must match " + ID_PATTERN.pattern() + ": '" + id + "'");
		}
		if (title.isEmpty()) {
			throw new IllegalArgumentException("Dockable title must not be empty");
		}
		this.id = id;
		this.title = title;
	}

	public DockableOptions icon(@Nullable ImageIcon icon) {
		this.icon = icon;
		return this;
	}

	/**
	 * Panel position on first show
	 */
	public DockableOptions defaultArea(DockArea area) {
		this.defaultArea = area;
		return this;
	}

	/**
	 * Allow user to close panel
	 */
	public DockableOptions closable(boolean closable) {
		this.closable = closable;
		return this;
	}

	/**
	 * Show panel on first run (saved layout used on next runs)
	 */
	public DockableOptions showOnStartup(boolean show) {
		this.showOnStartup = show;
		return this;
	}

	/**
	 * Add toggle entry to 'View' menu
	 */
	public DockableOptions menuEntry(boolean menuEntry) {
		this.menuEntry = menuEntry;
		return this;
	}

	/**
	 * Optional toggle keybinding {@link KeyStroke#getKeyStroke(String)}
	 */
	public DockableOptions keyBinding(@Nullable String keyBinding) {
		this.keyBinding = keyBinding;
		return this;
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public @Nullable ImageIcon getIcon() {
		return icon;
	}

	public DockArea getDefaultArea() {
		return defaultArea;
	}

	public boolean isClosable() {
		return closable;
	}

	public boolean isShowOnStartup() {
		return showOnStartup;
	}

	public boolean isMenuEntry() {
		return menuEntry;
	}

	public @Nullable String getKeyBinding() {
		return keyBinding;
	}
}
