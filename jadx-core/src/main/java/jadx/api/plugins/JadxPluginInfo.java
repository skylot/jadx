package jadx.api.plugins;

import org.jetbrains.annotations.Nullable;

public class JadxPluginInfo {
	private final String pluginId;
	private final String name;
	private final String description;
	private String homepage;

	/**
	 * Conflicting plugins should have the same 'provides' property; only one will be loaded
	 */
	private String provides;

	/**
	 * Minimum required jadx version to run this plugin.
	 * <br>
	 * Format: "<stable version>, r<revision number of unstable version>".
	 * Example: "1.5.1, r2305"
	 *
	 * @see <a href="https://github.com/skylot/jadx/wiki/Jadx-plugins-guide#required-jadx-version">wiki
	 *      page</a>
	 *      for details.
	 */
	private @Nullable String requiredJadxVersion;

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

	public void setHomepage(String homepage) {
		this.homepage = homepage;
	}

	public String getProvides() {
		return provides;
	}

	public void setProvides(String provides) {
		this.provides = provides;
	}

	public @Nullable String getRequiredJadxVersion() {
		return requiredJadxVersion;
	}

	public void setRequiredJadxVersion(@Nullable String requiredJadxVersion) {
		this.requiredJadxVersion = requiredJadxVersion;
	}

	@Override
	public String toString() {
		return pluginId + ": " + name + " - '" + description + '\'';
	}
}
