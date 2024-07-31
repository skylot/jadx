package jadx.gui.ui;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import jadx.gui.utils.UiUtils;

public class JadxEventQueue extends EventQueue {
	@Override
	protected void dispatchEvent(AWTEvent event) {
		event = mapEvent(event);
		super.dispatchEvent(event);
	}

	private static AWTEvent mapEvent(AWTEvent e) {
		if (UiUtils.isXToolkit() && e instanceof MouseEvent && ((MouseEvent) e).getButton() > 3) {
			return mapXWindowMouseEvent((MouseEvent) e);
		} else {
			return e;
		}
	}

	private static AWTEvent mapXWindowMouseEvent(MouseEvent src) {
		if (src.getButton() < 6) {
			// buttons 4-5 come from touchpad, they must be converted to horizontal scrolling events
			@SuppressWarnings("deprecation")
			int modifiers = src.getModifiers() | InputEvent.SHIFT_DOWN_MASK;
			// noinspection MagicConstant
			return new MouseWheelEvent(src.getComponent(), MouseEvent.MOUSE_WHEEL, src.getWhen(), modifiers,
					src.getX(), src.getY(), 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL,
					src.getClickCount(), src.getButton() == 4 ? -1 : 1);
		} else {
			// Here we "shift" events with buttons `6` and `7` to similar events with buttons 4 and 5
			@SuppressWarnings("deprecation")
			int modifiers = src.getModifiers() | (1 << (8 + src.getButton()));
			// noinspection MagicConstant
			return new MouseEvent(src.getComponent(), src.getID(), src.getWhen(), modifiers,
					src.getX(), src.getY(), 1, src.isPopupTrigger(), src.getButton() - 2);
		}
	}
}
