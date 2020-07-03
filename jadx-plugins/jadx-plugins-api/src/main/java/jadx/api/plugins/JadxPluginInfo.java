package jadx.api.plugins;

public class JadxPluginInfo {
	private final String pluginId;
	private final String name;
	private final String description;

	public JadxPluginInfo(String id, String name, String description) {
		this.pluginId = id;
		this.name = name;
		this.description = description;
	}

	public String getPluginId() {
		return pluginId;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return name + " - '" + description + '\'';
	}
}
