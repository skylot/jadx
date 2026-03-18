package jadx.gui.settings;

import java.awt.Rectangle;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class WindowLocation {
	private String windowId;
	private @Nullable Rectangle bounds;

	// Don't remove. Used in JSON serialization
	public WindowLocation() {
	}

	public WindowLocation(String windowId, @Nullable Rectangle bounds) {
		this.windowId = windowId;
		this.bounds = bounds;
	}

	public String getWindowId() {
		return windowId;
	}

	public void setWindowId(String windowId) {
		this.windowId = windowId;
	}

	public @Nullable Rectangle getBounds() {
		return bounds;
	}

	public void setBounds(@Nullable Rectangle bounds) {
		this.bounds = bounds;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(windowId);
	}

	@Override
	public final boolean equals(Object o) {
		if (o instanceof WindowLocation) {
			WindowLocation that = (WindowLocation) o;
			return Objects.equals(windowId, that.windowId)
					&& Objects.equals(bounds, that.bounds);
		}
		return false;
	}

	@Override
	public String toString() {
		return "WindowLocation{id=" + windowId + ", bounds=" + bounds + '}';
	}
}
