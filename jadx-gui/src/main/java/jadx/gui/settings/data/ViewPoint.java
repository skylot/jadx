package jadx.gui.settings.data;

import java.awt.Point;

public class ViewPoint {

	private int x;
	private int y;

	public ViewPoint() {
		this(0, 0);
	}

	public ViewPoint(Point p) {
		this(p.x, p.y);
	}

	public ViewPoint(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public Point toPoint() {
		return new Point(x, y);
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	@Override
	public String toString() {
		return "ViewPoint{" + x + ", " + y + '}';
	}
}
