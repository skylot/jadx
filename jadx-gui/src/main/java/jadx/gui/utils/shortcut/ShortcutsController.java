package jadx.gui.utils.shortcut;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.jetbrains.annotations.Nullable;

import jadx.gui.settings.JadxSettings;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.action.ActionModel;
import jadx.gui.ui.action.JadxGuiAction;
import jadx.gui.utils.UiUtils;

public class ShortcutsController {
	private Map<ActionModel, Shortcut> shortcuts;
	private final JadxSettings settings;

	private final Map<ActionModel, Set<JadxGuiAction>> boundActions = new HashMap<>();

	private Set<ActionModel> mouseActions = null;

	public ShortcutsController(JadxSettings settings) {
		this.settings = settings;
	}

	public void loadSettings() {
		this.shortcuts = settings.getShortcuts();

		indexMouseActions();

		for (Map.Entry<ActionModel, Shortcut> shortcutEntry : shortcuts.entrySet()) {
			ActionModel actionModel = shortcutEntry.getKey();
			Shortcut shortcut = shortcutEntry.getValue();
			Set<JadxGuiAction> actions = boundActions.get(actionModel);
			if (actions != null) {
				for (JadxGuiAction action : actions) {
					if (action != null) {
						action.setShortcut(shortcut);
					}
				}
			}
		}
	}

	@Nullable
	public Shortcut get(ActionModel actionModel) {
		return shortcuts.get(actionModel);
	}

	public KeyStroke getKeyStroke(ActionModel actionModel) {
		Shortcut shortcut = get(actionModel);
		KeyStroke keyStroke = null;
		if (shortcut != null) {
			keyStroke = shortcut.toKeyStroke();
		}
		return keyStroke;
	}

	/*
	 * Binds to an action and updates its shortcut every time loadSettings is called
	 */
	public void bind(JadxGuiAction action) {
		boundActions.computeIfAbsent(action.getActionModel(), k -> new HashSet<>());
		boundActions.get(action.getActionModel()).add(action);
	}

	/*
	 * Immediately sets the shortcut for an action
	 */
	public void bindImmediate(JadxGuiAction action) {
		bind(action);
		Shortcut shortcut = shortcuts.get(action.getActionModel());
		action.setShortcut(shortcut);
	}

	public static Map<ActionModel, Shortcut> getDefault() {
		Map<ActionModel, Shortcut> shortcuts = new HashMap<>();
		for (ActionModel actionModel : ActionModel.values()) {
			shortcuts.put(actionModel, actionModel.defaultShortcut);
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
					Set<JadxGuiAction> actions = boundActions.get(actionModel);
					if (actions != null) {
						for (JadxGuiAction action : actions) {
							if (action != null) {
								System.out.println("INFO  - performing action " + action);
								UiUtils.uiRun(action::performAction);
							}
						}
					}
				}
			}
		}, AWTEvent.MOUSE_EVENT_MASK);
	}

	private void indexMouseActions() {
		mouseActions = new HashSet<>();
		for (Map.Entry<ActionModel, Shortcut> shortcutEntry : shortcuts.entrySet()) {
			ActionModel actionModel = shortcutEntry.getKey();
			Shortcut shortcut = shortcutEntry.getValue();
			if (shortcut != null && shortcut.isMouse()) {
				mouseActions.add(actionModel);
			} else {
				mouseActions.remove(actionModel);
			}
		}
	}

	public void unbindActionsForComponent(JComponent component) {
		for (Map.Entry<ActionModel, Shortcut> shortcutEntry : shortcuts.entrySet()) {
			ActionModel actionModel = shortcutEntry.getKey();
			Set<JadxGuiAction> actions = boundActions.get(actionModel);
			if (actions != null) {
				actions.removeIf(action -> action != null && action.getTargetComponent() == component);
			}
		}
	}
}
