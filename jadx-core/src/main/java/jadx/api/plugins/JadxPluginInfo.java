package jadx.api.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

	/**
	 * ID of parent plugin. If set, this plugin will be loaded in a {@link ClassLoader}
	 * inherited from the parent plugin's class loader, allowing direct access to its classes.
	 */
	private @Nullable String parentPlugin;

	/**
	 * List of required plugin IDs (or location IDs) that should be installed and loaded
	 * before this plugin.
	 */
	private List<String> dependencyPlugins = Collections.emptyList();

	/**
	 * Optional mapping from dependency pluginId to install locationId for plugins not in
	 * jadx-plugins-list.
	 */
	private Map<String, String> dependencyLocations = Collections.emptyMap();

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

	public @Nullable String getParentPlugin() {
		return parentPlugin;
	}

	public void setParentPlugin(@Nullable String parentPlugin) {
		this.parentPlugin = parentPlugin;
	}

	public List<String> getDependencyPlugins() {
		return dependencyPlugins;
	}

	public void setDependencyPlugins(@Nullable List<String> dependencyPlugins) {
		if (dependencyPlugins == null || dependencyPlugins.isEmpty()) {
			this.dependencyPlugins = Collections.emptyList();
		} else {
			this.dependencyPlugins = Collections.unmodifiableList(new ArrayList<>(dependencyPlugins));
		}
	}

	public Map<String, String> getDependencyLocations() {
		return dependencyLocations;
	}

	public void setDependencyLocations(@Nullable Map<String, String> dependencyLocations) {
		if (dependencyLocations == null || dependencyLocations.isEmpty()) {
			this.dependencyLocations = Collections.emptyMap();
		} else {
			this.dependencyLocations = Collections.unmodifiableMap(new LinkedHashMap<>(dependencyLocations));
		}
	}

	@Override
	public String toString() {
		return pluginId + ": " + name + " - '" + description + '\'';
	}
}
