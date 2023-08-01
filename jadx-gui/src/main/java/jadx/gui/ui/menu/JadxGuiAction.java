package jadx.gui.ui.menu;

import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.shortcut.Shortcut;
import jadx.gui.utils.ui.ActionHandler;

public class JadxGuiAction extends ActionHandler {
	private ActionModel actionModel;

	public JadxGuiAction(ActionModel actionModel, Runnable action) {
		super(action);

		this.actionModel = actionModel;

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

	public JadxGuiAction(ActionModel actionId, Consumer<ActionEvent> consumer) {
		super(consumer);
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
}
