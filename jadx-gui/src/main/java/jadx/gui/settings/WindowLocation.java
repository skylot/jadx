package jadx.gui.settings;

public class WindowLocation {

	private final String windowId;

	private final int x;
	private final int y;
	private final int width;
	private final int height;

	public WindowLocation(String windowId, int x, int y, int width, int height) {
		this.windowId = windowId;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public String getWindowId() {
		return windowId;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	@Override
	public String toString() {
		return "WindowLocation{" +
				"id='" + windowId + '\'' +
				", x=" + x +
				", y=" + y +
				", width=" + width +
				", height=" + height +
				'}';
	}
}
