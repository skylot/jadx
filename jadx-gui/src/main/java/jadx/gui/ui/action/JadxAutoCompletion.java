package jadx.gui.ui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;

import jadx.gui.utils.shortcut.Shortcut;

public class JadxAutoCompletion extends AutoCompletion
		implements IShortcutAction {
	public static final String COMMAND = "JadxAutoCompletion.Command";

	/**
	 * Constructor.
	 *
	 * @param provider The completion provider. This cannot be <code>null</code>
	 */
	public JadxAutoCompletion(CompletionProvider provider) {
		super(provider);
	}

	@Override
	public ActionModel getActionModel() {
		return ActionModel.SCRIPT_AUTO_COMPLETE;
	}

	@Override
	public JComponent getShortcutComponent() {
		return getTextComponent();
	}

	@Override
	public void performAction() {
		createAutoCompleteAction().actionPerformed(
				new ActionEvent(this, ActionEvent.ACTION_PERFORMED, COMMAND));
	}

	@Override
	public void setShortcut(Shortcut shortcut) {
		if (shortcut != null && shortcut.isKeyboard()) {
			setTriggerKey(shortcut.toKeyStroke());
		} else {
			setTriggerKey(KeyStroke.getKeyStroke(KeyEvent.VK_UNDEFINED, 0));
		}
	}
}
