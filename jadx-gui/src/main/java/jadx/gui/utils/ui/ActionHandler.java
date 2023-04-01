package jadx.gui.utils.ui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import jadx.gui.utils.UiUtils;

public class ActionHandler extends AbstractAction {

	private final Consumer<ActionEvent> consumer;

	public ActionHandler(Runnable action) {
		this.consumer = ev -> action.run();
	}

	public ActionHandler(Consumer<ActionEvent> consumer) {
		this.consumer = consumer;
	}

	public void setName(String name) {
		putValue(NAME, name);
	}

	public ActionHandler withNameAndDesc(String name) {
		setNameAndDesc(name);
		return this;
	}

	public void setNameAndDesc(String name) {
		setName(name);
		setShortDescription(name);
	}

	public void setShortDescription(String desc) {
		putValue(SHORT_DESCRIPTION, desc);
	}

	public void setIcon(ImageIcon icon) {
		putValue(SMALL_ICON, icon);
	}

	public void setKeyBinding(KeyStroke keyStroke) {
		putValue(ACCELERATOR_KEY, keyStroke);
	}

	public void attachKeyBindingFor(JComponent component, KeyStroke keyStroke) {
		UiUtils.addKeyBinding(component, keyStroke, "run", this);
		setKeyBinding(keyStroke);
	}

	public void addKeyBindToDescription() {
		KeyStroke keyStroke = (KeyStroke) getValue(ACCELERATOR_KEY);
		if (keyStroke != null) {
			String keyText = KeyEvent.getKeyText(keyStroke.getKeyCode());
			String desc = (String) getValue(SHORT_DESCRIPTION);
			setShortDescription(desc + " (" + keyText + ")");
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		consumer.accept(e);
	}

	public JButton makeButton() {
		addKeyBindToDescription();
		return new JButton(this);
	}
}
