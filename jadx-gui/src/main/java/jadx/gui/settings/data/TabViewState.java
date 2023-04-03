package jadx.gui.settings.data;

public class TabViewState {
	private String type;
	private String tabPath;
	private String subPath;
	private int caret;
	private ViewPoint view;
	boolean active;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTabPath() {
		return tabPath;
	}

	public void setTabPath(String tabPath) {
		this.tabPath = tabPath;
	}

	public String getSubPath() {
		return subPath;
	}

	public void setSubPath(String subPath) {
		this.subPath = subPath;
	}

	public int getCaret() {
		return caret;
	}

	public void setCaret(int caret) {
		this.caret = caret;
	}

	public ViewPoint getView() {
		return view;
	}

	public void setView(ViewPoint view) {
		this.view = view;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
