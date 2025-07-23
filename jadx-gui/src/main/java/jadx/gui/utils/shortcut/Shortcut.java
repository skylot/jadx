package jadx.gui.utils.shortcut;

import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.swing.KeyStroke;

import jadx.commons.app.JadxSystemInfo;

public class Shortcut {
	private static final Set<Integer> FORBIDDEN_KEY_CODES = new HashSet<>(List.of(
			KeyEvent.VK_UNDEFINED, KeyEvent.VK_SHIFT, KeyEvent.VK_ALT, KeyEvent.VK_META, KeyEvent.VK_ALT_GRAPH));
	private static final Set<Integer> ALLOWED_MODIFIERS = new HashSet<>(List.of(
			KeyEvent.CTRL_DOWN_MASK, KeyEvent.META_DOWN_MASK, KeyEvent.ALT_DOWN_MASK, KeyEvent.ALT_GRAPH_DOWN_MASK,
			KeyEvent.SHIFT_DOWN_MASK));

	private Integer keyCode = null;
	private Integer modifiers = null;
	private Integer mouseButton = null;

	private Shortcut() {
	}

	public static Shortcut keyboard(int keyCode) {
		return keyboard(keyCode, 0);
	}

	public static Shortcut keyboard(int keyCode, int modifiers) {
		Shortcut shortcut = new Shortcut();
		shortcut.keyCode = keyCode;
		shortcut.modifiers = modifiers;
		return shortcut;
	}

	public static Shortcut mouse(int mouseButton) {
		Shortcut shortcut = new Shortcut();
		shortcut.mouseButton = mouseButton;
		return shortcut;
	}

	public static Shortcut none() {
		Shortcut shortcut = new Shortcut();
		// Must have at least one non-null attribute in order to be serialized
		// otherwise will roll back to default Shortcut
		shortcut.modifiers = 0;
		return shortcut;
	}

	public Integer getKeyCode() {
		return keyCode;
	}

	public Integer getModifiers() {
		return modifiers;
	}

	public Integer getMouseButton() {
		return mouseButton;
	}

	public boolean isKeyboard() {
		return keyCode != null;
	}

	public boolean isMouse() {
		return mouseButton != null;
	}

	public boolean isNone() {
		return !isMouse() && !isKeyboard();
	}

	public boolean isValidKeyboard() {
		return isKeyboard() && !FORBIDDEN_KEY_CODES.contains(keyCode) && isValidModifiers();
	}

	public boolean isValidModifiers() {
		int modifiersTest = modifiers;
		for (Integer modifier : ALLOWED_MODIFIERS) {
			modifiersTest &= ~modifier;
		}
		return modifiersTest == 0;
	}

	public KeyStroke toKeyStroke() {
		return isKeyboard()
				? KeyStroke.getKeyStroke(keyCode, modifiers,
						modifiers != 0 && JadxSystemInfo.IS_MAC)
				: null;
	}

	@Override
	public String toString() {
		if (isKeyboard()) {
			return keyToString();
		} else if (isMouse()) {
			return mouseToString();
		}
		return "NONE";
	}

	public String getTypeString() {
		if (isKeyboard()) {
			return "Keyboard";
		} else if (isMouse()) {
			return "Mouse";
		}
		return null;
	}

	private String mouseToString() {
		return "MouseButton" + mouseButton;
	}

	private String keyToString() {
		StringBuilder sb = new StringBuilder();
		if (modifiers != null && modifiers > 0) {
			sb.append(KeyEvent.getModifiersExText(modifiers));
			sb.append('+');
		}
		if (keyCode != null && keyCode != 0) {
			sb.append(KeyEvent.getKeyText(keyCode));
		} else {
			sb.append("UNDEFINED");
		}
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Shortcut shortcut = (Shortcut) o;
		return Objects.equals(keyCode, shortcut.keyCode)
				&& Objects.equals(modifiers, shortcut.modifiers)
				&& Objects.equals(mouseButton, shortcut.mouseButton);
	}

	@Override
	public int hashCode() {
		return Objects.hash(keyCode, modifiers, mouseButton);
	}
}
