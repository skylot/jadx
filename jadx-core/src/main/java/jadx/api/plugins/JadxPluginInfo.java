package jadx.api.plugins;

public class JadxPluginInfo {
	private final String pluginId;
	private final String name;
	private final String description;
	private final String homepage;

	/**
	 * Conflicting plugins should have the same 'provides' property; only one will be loaded
	 */
	private final String provides;

	public JadxPluginInfo(String id, String name, String description) {
		this(id, name, description, "", id);
	}

	public JadxPluginInfo(String pluginId, String name, String description, String provides) {
		this(pluginId, name, description, "", provides);
	}

	public JadxPluginInfo(String pluginId, String name, String description, String homepage, String provides) {
		this.pluginId = pluginId;
		this.name = name;
		this.description = description;
		this.homepage = homepage;
		this.provides = provides;
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

	public String getHomepage() {
		return homepage;
	}

	public String getProvides() {
		return provides;
	}

	@Override
	public String toString() {
		return pluginId + ": " + name + " - '" + description + '\'';
	}
}
