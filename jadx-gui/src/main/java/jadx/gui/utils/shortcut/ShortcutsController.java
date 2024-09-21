package jadx.gui.utils.shortcut;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.data.ShortcutsWrapper;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.action.ActionCategory;
import jadx.gui.ui.action.ActionModel;
import jadx.gui.ui.action.IShortcutAction;
import jadx.gui.utils.UiUtils;

public class ShortcutsController {
	private static final Logger LOG = LoggerFactory.getLogger(ShortcutsController.class);

	private final JadxSettings settings;
	private final Map<ActionModel, Set<IShortcutAction>> boundActions = new EnumMap<>(ActionModel.class);
	private final Set<ActionModel> mouseActions = EnumSet.noneOf(ActionModel.class);

	private ShortcutsWrapper shortcuts;

	public ShortcutsController(JadxSettings settings) {
		this.settings = settings;
	}

	public void loadSettings() {
		shortcuts = settings.getShortcuts();
		indexMouseActions();
		boundActions.forEach((actionModel, actions) -> {
			if (actions != null) {
				Shortcut shortcut = get(actionModel);
				for (IShortcutAction action : actions) {
					action.setShortcut(shortcut);
				}
			}
		});
	}

	@Nullable
	public Shortcut get(ActionModel actionModel) {
		return shortcuts.get(actionModel);
	}

	public KeyStroke getKeyStroke(ActionModel actionModel) {
		Shortcut shortcut = get(actionModel);
		if (shortcut != null && shortcut.isKeyboard()) {
			return shortcut.toKeyStroke();
		}
		return null;
	}

	/**
	 * Binds to an action and updates its shortcut every time loadSettings is called
	 */
	public void bind(IShortcutAction action) {
		if (action.getShortcutComponent() == null) {
			LOG.warn("No shortcut component in action: {}", action, new JadxRuntimeException());
			return;
		}
		boundActions.computeIfAbsent(action.getActionModel(), k -> new HashSet<>());
		boundActions.get(action.getActionModel()).add(action);
	}

	/*
	 * Immediately sets the shortcut for an action
	 */
	public void bindImmediate(IShortcutAction action) {
		bind(action);
		Shortcut shortcut = get(action.getActionModel());
		action.setShortcut(shortcut);
	}

	public static Map<ActionModel, Shortcut> getDefault() {
		Map<ActionModel, Shortcut> shortcuts = new HashMap<>();
		for (ActionModel actionModel : ActionModel.values()) {
			shortcuts.put(actionModel, actionModel.getDefaultShortcut());
		}
		return shortcuts;
	}

	public void registerMouseEventListener(MainWindow mw) {
		Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
			if (mw.isSettingsOpen()) {
				return;
			}
			if (!(event instanceof MouseEvent)) {
				return;
			}
			MouseEvent mouseEvent = (MouseEvent) event;
			if (mouseEvent.getID() != MouseEvent.MOUSE_PRESSED) {
				return;
			}
			int mouseButton = mouseEvent.getButton();
			for (ActionModel actionModel : mouseActions) {
				Shortcut shortcut = shortcuts.get(actionModel);
				if (shortcut != null && shortcut.getMouseButton() == mouseButton) {
					Set<IShortcutAction> actions = boundActions.get(actionModel);
					if (actions != null) {
						for (IShortcutAction action : actions) {
							if (action != null) {
								mouseEvent.consume();
								UiUtils.uiRun(action::performAction);
							}
						}
					}
				}
			}
		}, AWTEvent.MOUSE_EVENT_MASK);
	}

	private void indexMouseActions() {
		mouseActions.clear();
		for (ActionModel actionModel : ActionModel.values()) {
			Shortcut shortcut = shortcuts.get(actionModel);
			if (shortcut != null && shortcut.isMouse()) {
				mouseActions.add(actionModel);
			}
		}
	}

	public void unbindActionsForComponent(JComponent component) {
		for (Set<IShortcutAction> actions : boundActions.values()) {
			if (actions != null) {
				actions.removeIf(action -> action == null
						|| action.getShortcutComponent() == null
						|| action.getShortcutComponent() == component);
			}
		}
	}

	/**
	 * Keep only actions bound to the main window.
	 * Other actions will be added on demand.
	 */
	public void reset() {
		for (ActionModel actionModel : ActionModel.values()) {
			if (actionModel.getCategory() != ActionCategory.MENU_TOOLBAR) {
				boundActions.remove(actionModel);
			}
		}
	}
}
