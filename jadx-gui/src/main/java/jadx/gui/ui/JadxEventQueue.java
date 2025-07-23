package jadx.gui.ui;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import jadx.commons.app.JadxSystemInfo;

public class JadxEventQueue extends EventQueue {

	private static final boolean IS_X_TOOLKIT = JadxSystemInfo.IS_LINUX
			&& "sun.awt.X11.XToolkit".equals(Toolkit.getDefaultToolkit().getClass().getName());

	public static void register() {
		if (IS_X_TOOLKIT) {
			Toolkit.getDefaultToolkit().getSystemEventQueue().push(new JadxEventQueue());
		}
	}

	private JadxEventQueue() {
	}

	@Override
	protected void dispatchEvent(AWTEvent event) {
		AWTEvent mappedEvent = mapEvent(event);
		super.dispatchEvent(mappedEvent);
	}

	private static AWTEvent mapEvent(AWTEvent event) {
		if (IS_X_TOOLKIT && event instanceof MouseEvent && ((MouseEvent) event).getButton() > 3) {
			return mapXWindowMouseEvent((MouseEvent) event);
		}
		return event;
	}

	@SuppressWarnings({ "deprecation", "MagicConstant" })
	private static AWTEvent mapXWindowMouseEvent(MouseEvent src) {
		if (src.getButton() < 6) {
			// buttons 4-5 come from touchpad, they must be converted to horizontal scrolling events
			int modifiers = src.getModifiers() | InputEvent.SHIFT_DOWN_MASK;
			return new MouseWheelEvent(src.getComponent(), MouseEvent.MOUSE_WHEEL, src.getWhen(), modifiers,
					src.getX(), src.getY(), 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL,
					src.getClickCount(), src.getButton() == 4 ? -1 : 1);
		} else {
			// Here we "shift" events with buttons `6` and `7` to similar events with buttons 4 and 5
			// See `java.awt.InputEvent#BUTTON_DOWN_MASK`, 1<<14 is the 4th physical button, 1<<15 is the 5th.
			int modifiers = src.getModifiers() | (1 << (8 + src.getButton()));
			return new MouseEvent(src.getComponent(), src.getID(), src.getWhen(), modifiers,
					src.getX(), src.getY(), 1, src.isPopupTrigger(), src.getButton() - 2);
		}
	}
}
