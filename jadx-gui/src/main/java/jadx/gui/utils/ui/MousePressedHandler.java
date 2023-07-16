package jadx.gui.utils.ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class MousePressedHandler extends MouseAdapter {

	private final Consumer<MouseEvent> listener;

	public MousePressedHandler(Consumer<MouseEvent> listener) {
		this.listener = listener;
	}

	@Override
	public void mousePressed(MouseEvent ev) {
		listener.accept(ev);
	}
}
