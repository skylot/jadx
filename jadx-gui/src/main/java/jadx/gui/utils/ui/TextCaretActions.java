package jadx.gui.utils.ui;

import java.awt.event.ActionEvent;
import java.util.function.Supplier;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

public class TextCaretActions {

	public static void install() {
		install("TextField", JTextField::new);
		install("FormattedTextField", JFormattedTextField::new);
		install("PasswordField", JPasswordField::new);
		install("TextArea", JTextArea::new);
		install("TextPane", JTextPane::new);
		install("EditorPane", JEditorPane::new);
	}

	private static void install(String uiPrefix, Supplier<JTextComponent> componentSupplier) {
		String mapName = uiPrefix + ".actionMap";
		if (UIManager.get(mapName) == null) {
			// actions map created and saved into look and feel defaults on first component init
			componentSupplier.get();
		}
		Object map = UIManager.get(mapName);
		if (map instanceof ActionMap) {
			ActionMap actionMap = (ActionMap) map;
			wrapAction(actionMap, DefaultEditorKit.backwardAction, true);
			wrapAction(actionMap, DefaultEditorKit.forwardAction, false);
		}
	}

	private static void wrapAction(ActionMap actionMap, String name, boolean toSelectionStart) {
		Action action = actionMap.get(name);
		if (action != null && !(action instanceof SelectionCaretAction)) {
			actionMap.put(name, new SelectionCaretAction(name, action, toSelectionStart));
		}
	}

	private static final class SelectionCaretAction extends TextAction {
		private static final long serialVersionUID = -2384125906375281853L;

		private final transient Action action;
		private final boolean toSelectionStart;

		private SelectionCaretAction(String name, Action action, boolean toSelectionStart) {
			super(name);
			this.action = action;
			this.toSelectionStart = toSelectionStart;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			JTextComponent textCmp = getTextComponent(e);
			if (textCmp != null && textCmp.getComponentOrientation().isLeftToRight()) {
				int start = textCmp.getSelectionStart();
				int end = textCmp.getSelectionEnd();
				if (start != end) {
					textCmp.setCaretPosition(toSelectionStart ? start : end);
					return;
				}
			}
			action.actionPerformed(e);
		}
	}
}
