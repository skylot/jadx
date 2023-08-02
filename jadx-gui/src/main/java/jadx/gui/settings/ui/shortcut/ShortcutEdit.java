package jadx.gui.settings.ui.shortcut;

import java.awt.AWTEvent;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.ui.JadxSettingsWindow;
import jadx.gui.ui.menu.ActionModel;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.shortcut.Shortcut;

public class ShortcutEdit extends JPanel {
	private static final Icon CLEAR_ICON = UiUtils.openSvgIcon("ui/close");

	private final ActionModel actionModel;
	private final JadxSettingsWindow settingsWindow;
	private final JadxSettings settings;
	private final TextField textField;

	public Shortcut shortcut;

	public ShortcutEdit(ActionModel actionModel, JadxSettingsWindow settingsWindow, JadxSettings settings) {
		this.actionModel = actionModel;
		this.settings = settings;
		this.settingsWindow = settingsWindow;

		textField = new TextField();
		JButton clearButton = new JButton(CLEAR_ICON);

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(textField);
		add(clearButton);

		clearButton.addActionListener(e -> {
			setShortcut(null);
			saveShortcut();
		});
	}

	public void setShortcut(Shortcut shortcut) {
		this.shortcut = shortcut;
		textField.reload();
	}

	private void saveShortcut() {
		settings.getShortcuts().put(actionModel, shortcut);
		settingsWindow.needReload();
	}

	private class TextField extends JTextField {
		private Shortcut tempShortcut;

		public TextField() {
			KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(ev -> {
				if (!isListening()) {
					return false;
				}

				if (ev.getID() == KeyEvent.KEY_PRESSED) {
					Shortcut pressedShortcut = Shortcut.keyboard(ev.getKeyCode(), ev.getModifiersEx());
					if (pressedShortcut.isValidKeyboard()) {
						tempShortcut = pressedShortcut;
						refresh(tempShortcut);
					} else {
						tempShortcut = null;
					}
				} else if (ev.getID() == KeyEvent.KEY_TYPED) {
					removeFocus();
				}
				ev.consume();
				return true;
			});

			addFocusListener(new FocusListener() {
				@Override
				public void focusGained(FocusEvent ev) {
				}

				@Override
				public void focusLost(FocusEvent ev) {
					if (tempShortcut != null) {
						shortcut = tempShortcut;
						tempShortcut = null;
						saveShortcut();
					}
				}
			});

			Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
				if (!isListening()) {
					return;
				}

				if (event instanceof MouseEvent) {
					MouseEvent mouseEvent = (MouseEvent) event;
					if (mouseEvent.getID() == MouseEvent.MOUSE_PRESSED) {
						int mouseButton = mouseEvent.getButton();

						if (mouseButton == MouseEvent.NOBUTTON) {
							return;
						}

						if (mouseButton <= MouseEvent.BUTTON3) {
							// TODO show warning that this is a commonly used key
							return;
						}

						shortcut = Shortcut.mouse(mouseButton);
						reload();
						saveShortcut();
					}
				}
			}, AWTEvent.MOUSE_EVENT_MASK);
		}

		public void reload() {
			refresh(shortcut);
		}

		private void refresh(Shortcut displayedShortcut) {
			if (displayedShortcut == null) {
				setText("None");
				setForeground(UIManager.getColor("TextArea.inactiveForeground"));
				return;
			}
			setText(displayedShortcut.toString());
			setForeground(UIManager.getColor("TextArea.foreground"));
		}

		private void removeFocus() {
			// triggers focusLost
			getRootPane().requestFocus();
		}

		private boolean isListening() {
			return isFocusOwner();
		}
	}
}