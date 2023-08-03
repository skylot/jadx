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
import jadx.gui.settings.data.ShortcutsWrapper;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.action.ActionModel;
import jadx.gui.ui.action.IShortcutAction;
import jadx.gui.utils.UiUtils;

public class ShortcutsController {
	private ShortcutsWrapper shortcuts;
	private final JadxSettings settings;

	private final Map<ActionModel, Set<IShortcutAction>> boundActions = new HashMap<>();

	private Set<ActionModel> mouseActions = null;

	public ShortcutsController(JadxSettings settings) {
		this.settings = settings;
	}

	public void loadSettings() {
		this.shortcuts = settings.getShortcuts();

		indexMouseActions();

		for (Map.Entry<ActionModel, Set<IShortcutAction>> actionsEntry : boundActions.entrySet()) {
			ActionModel actionModel = actionsEntry.getKey();
			Set<IShortcutAction> actions = actionsEntry.getValue();
			Shortcut shortcut = get(actionModel);
			if (actions != null) {
				for (IShortcutAction action : actions) {
					action.setShortcut(shortcut);
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
		if (shortcut != null && shortcut.isKeyboard()) {
			keyStroke = shortcut.toKeyStroke();
		}
		return keyStroke;
	}

	/*
	 * Binds to an action and updates its shortcut every time loadSettings is called
	 */
	public void bind(IShortcutAction action) {
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
		mouseActions = new HashSet<>();
		for (ActionModel actionModel : ActionModel.values()) {
			Shortcut shortcut = shortcuts.get(actionModel);
			if (shortcut != null && shortcut.isMouse()) {
				mouseActions.add(actionModel);
			} else {
				mouseActions.remove(actionModel);
			}
		}
	}

	public void unbindActionsForComponent(JComponent component) {
		for (ActionModel actionModel : ActionModel.values()) {
			Set<IShortcutAction> actions = boundActions.get(actionModel);
			if (actions != null) {
				actions.removeIf(action -> action != null && action.getShortcutComponent() == component);
			}
		}
	}
}
