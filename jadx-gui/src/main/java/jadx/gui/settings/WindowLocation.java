package jadx.gui.settings;

import java.awt.Rectangle;

public class WindowLocation {
	private String windowId;
	private Rectangle bounds;

	// Don't remove. Used in json serialization
	public WindowLocation() {
	}

	public WindowLocation(String windowId, Rectangle bounds) {
		this.windowId = windowId;
		this.bounds = bounds;
	}

	public String getWindowId() {
		return windowId;
	}

	public void setWindowId(String windowId) {
		this.windowId = windowId;
	}

	public Rectangle getBounds() {
		return bounds;
	}

	public void setBounds(Rectangle bounds) {
		this.bounds = bounds;
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
