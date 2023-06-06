package jadx.gui.settings.ui.plugins;

public class TitleNode extends BasePluginListNode {
	private final String title;

	public TitleNode(String title) {
		this.title = title;
	}

	@Override
	public String getTitle() {
		return title;
	}
}
