package jadx.gui.utils.ui;

import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

public class ActionHandler extends AbstractAction {

	private final Consumer<ActionEvent> consumer;

	public ActionHandler(Consumer<ActionEvent> consumer) {
		this.consumer = consumer;
	}

	public void setName(String name) {
		putValue(NAME, name);
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

	@Override
	public void actionPerformed(ActionEvent e) {
		consumer.accept(e);
	}
}
