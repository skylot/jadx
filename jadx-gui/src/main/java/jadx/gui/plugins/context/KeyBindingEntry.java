package jadx.gui.plugins.context;

import javax.swing.KeyStroke;

public class KeyBindingEntry {
	private final String id;
	private final KeyStroke keyStroke;
	private final Runnable action;

	public KeyBindingEntry(String id, KeyStroke keyStroke, Runnable action) {
		this.id = id;
		this.keyStroke = keyStroke;
		this.action = action;
	}

	public String getId() {
		return id;
	}

	public KeyStroke getKeyStroke() {
		return keyStroke;
	}

	public Runnable getAction() {
		return action;
	}
}
