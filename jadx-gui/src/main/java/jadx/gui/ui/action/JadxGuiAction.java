package jadx.gui.ui.action;

import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.jetbrains.annotations.Nullable;

import jadx.gui.utils.UiUtils;
import jadx.gui.utils.shortcut.Shortcut;
import jadx.gui.utils.ui.ActionHandler;

public class JadxGuiAction extends ActionHandler implements IShortcutAction {
	private static final String COMMAND = "JadxGuiAction.Command.%s";

	private final ActionModel actionModel;
	private final String id;
	private JComponent shortcutComponent = null;
	private KeyStroke addedKeyStroke = null;
	private Shortcut shortcut;

	public JadxGuiAction(ActionModel actionModel) {
		this.actionModel = actionModel;
		this.id = actionModel.name();

		updateProperties();
	}

	public JadxGuiAction(ActionModel actionModel, Runnable action) {
		super(action);
		this.actionModel = actionModel;
		this.id = actionModel.name();

		updateProperties();
	}

	public JadxGuiAction(ActionModel actionModel, Consumer<ActionEvent> consumer) {
		super(consumer);
		this.actionModel = actionModel;
		this.id = actionModel.name();

		updateProperties();
	}

	public JadxGuiAction(String id) {
		this.actionModel = null;
		this.id = id;

		updateProperties();
	}

	private void updateProperties() {
		if (actionModel == null) {
			return;
		}

		String name = actionModel.getName();
		String description = actionModel.getDescription();
		ImageIcon icon = actionModel.getIcon();
		if (name != null) {
			setName(name);
		}
		if (description != null) {
			setShortDescription(description);
		}
		if (icon != null) {
			setIcon(icon);
		}
	}

	@Nullable
	public ActionModel getActionModel() {
		return actionModel;
	}

	@Override
	public void setShortcut(Shortcut shortcut) {
		this.shortcut = shortcut;
		if (shortcut != null) {
			setKeyBinding(shortcut.toKeyStroke());
		} else {
			setKeyBinding(null);
		}
	}

	public void setShortcutComponent(JComponent component) {
		this.shortcutComponent = component;
	}

	@Override
	public JComponent getShortcutComponent() {
		return shortcutComponent;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
	}

	@Override
	public void performAction() {
		if (shortcutComponent != null && !shortcutComponent.isShowing()) {
			return;
		}

		String shortcutType = "null";
		if (shortcut != null) {
			shortcutType = shortcut.getTypeString();
		}
		actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
				String.format(COMMAND, shortcutType)));
	}

	public static boolean isSource(ActionEvent event) {
		return event.getActionCommand() != null
				&& event.getActionCommand().startsWith(String.format(COMMAND, ""));
	}

	@Override
	public void setKeyBinding(KeyStroke keyStroke) {
		if (shortcutComponent == null) {
			super.setKeyBinding(keyStroke);
		} else {
			// We just set the keyStroke for it to appear in the menu item
			// (grayed out in the right)
			super.setKeyBinding(keyStroke);

			if (addedKeyStroke != null) {
				UiUtils.removeKeyBinding(shortcutComponent, addedKeyStroke, id);
			}
			UiUtils.addKeyBinding(shortcutComponent, keyStroke, id, this::performAction);
			addedKeyStroke = keyStroke;
		}
	}

	@Override
	public String toString() {
		return "JadxGuiAction{" + id + ", component: " + shortcutComponent + '}';
	}
}
