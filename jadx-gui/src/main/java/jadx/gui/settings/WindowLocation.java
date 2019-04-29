package jadx.gui.settings;

import java.awt.*;

public class WindowLocation {

	private final String windowId;

	private final Rectangle bounds;

	public WindowLocation(String windowId, Rectangle bounds) {
		this.windowId = windowId;
		this.bounds = bounds;
	}

	public String getWindowId() {
		return windowId;
	}

	public Rectangle getBounds() {
		return bounds;
	}

	@Override
	public String toString() {
		return "WindowLocation{"
				+ "id='" + windowId + '\''
				+ ", x=" + bounds.getX()
				+ ", y=" + bounds.getY()
				+ ", width=" + bounds.getWidth()
				+ ", height=" + bounds.getHeight()
				+ '}';
	}
}
