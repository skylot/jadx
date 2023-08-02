package jadx.gui.ui.menu;

import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.shortcut.Shortcut;
import jadx.gui.utils.ui.ActionHandler;

public class JadxGuiAction extends ActionHandler {
	public static final String COMMAND = "JadxGuiAction.Command";

	private final ActionModel actionModel;
	private final String id;
	private JComponent targetComponent = null;
	private KeyStroke addedKeyStroke = null;

	public JadxGuiAction(ActionModel actionModel) {
		super();
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
		super();
		this.actionModel = null;
		this.id = id;

		updateProperties();
	}

	private void updateProperties() {
		if (actionModel == null) {
			return;
		}

		if (actionModel.nameRes != null) {
			setName(NLS.str(actionModel.nameRes));
		}
		if (actionModel.descRes != null) {
			setShortDescription(NLS.str(actionModel.descRes));
		}
		if (actionModel.iconPath != null) {
			setIcon(UiUtils.openSvgIcon(actionModel.iconPath));
		}
	}

	public ActionModel getActionModel() {
		return actionModel;
	}

	public void setShortcut(Shortcut shortcut) {
		if (shortcut != null) {
			setKeyBinding(shortcut.toKeyStroke());
		} else {
			setKeyBinding(null);
		}
	}

	public void setTargetComponent(JComponent component) {
		this.targetComponent = component;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (targetComponent != null && e.getSource() != targetComponent) {
			// We just add this keyStroke for visual appearance
			return;
		}
		super.actionPerformed(e);
	}

	@Override
	public void setKeyBinding(KeyStroke keyStroke) {
		if (targetComponent == null) {
			super.setKeyBinding(keyStroke);
		} else {
			// We just set the keyStroke for it to appear in the menu item
			// (grayed out in the right)
			super.setKeyBinding(keyStroke);

			if (addedKeyStroke != null) {
				UiUtils.removeKeyBinding(targetComponent, addedKeyStroke, id);
			}
			addedKeyStroke = keyStroke;
			UiUtils.addKeyBinding(targetComponent, keyStroke, id, () -> actionPerformed(
					new ActionEvent(targetComponent, ActionEvent.ACTION_PERFORMED, COMMAND)));
		}
	}
}
