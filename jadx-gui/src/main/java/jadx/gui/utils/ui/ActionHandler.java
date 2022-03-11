package jadx.gui.utils.ui;

import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import javax.swing.AbstractAction;

public class ActionHandler extends AbstractAction {

	private final Consumer<ActionEvent> consumer;

	public ActionHandler(Consumer<ActionEvent> consumer) {
		this.consumer = consumer;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		consumer.accept(e);
	}
}
