package jadx.api.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import jadx.core.plugins.versions.VerifyRequiredVersion;

public class JadxPluginInfoBuilder {
	private String pluginId;
	private String name;
	private String description;
	private String homepage = "";
	private @Nullable String requiredJadxVersion;
	private @Nullable String provides;
	private @Nullable String parentPlugin;
	private final List<String> dependencyPlugins = new ArrayList<>();
	private final Map<String, String> dependencyLocations = new LinkedHashMap<>();

	/**
	 * Start building method
	 */
	public static JadxPluginInfoBuilder pluginId(String pluginId) {
		JadxPluginInfoBuilder builder = new JadxPluginInfoBuilder();
		builder.pluginId = Objects.requireNonNull(pluginId);
		return builder;
	}

	private JadxPluginInfoBuilder() {
	}

	public JadxPluginInfoBuilder name(String name) {
		this.name = Objects.requireNonNull(name);
		return this;
	}

	public JadxPluginInfoBuilder description(String description) {
		this.description = Objects.requireNonNull(description);
		return this;
	}

	public JadxPluginInfoBuilder homepage(String homepage) {
		this.homepage = homepage;
		return this;
	}

	public JadxPluginInfoBuilder provides(String provides) {
		this.provides = provides;
		return this;
	}

	public JadxPluginInfoBuilder requiredJadxVersion(String versions) {
		this.requiredJadxVersion = versions;
		return this;
	}

	public JadxPluginInfoBuilder parentPlugin(@Nullable String parentPlugin) {
		this.parentPlugin = parentPlugin;
		return this;
	}

	public JadxPluginInfoBuilder dependencyPlugin(String pluginIdOrLocation) {
		Objects.requireNonNull(pluginIdOrLocation, "Dependency plugin is required");
		if (!dependencyPlugins.contains(pluginIdOrLocation)) {
			dependencyPlugins.add(pluginIdOrLocation);
		}
		return this;
	}

	public JadxPluginInfoBuilder dependencyPlugin(String pluginId, String locationId) {
		Objects.requireNonNull(pluginId, "Dependency pluginId is required");
		Objects.requireNonNull(locationId, "Dependency locationId is required");
		if (locationId.isBlank()) {
			throw new IllegalArgumentException("Empty locationId for dependency plugin: " + pluginId);
		}
		dependencyPlugin(pluginId);
		dependencyLocations.put(pluginId, locationId);
		return this;
	}

	public JadxPluginInfoBuilder dependencyPlugins(String... pluginIds) {
		Objects.requireNonNull(pluginIds);
		for (String pluginId : pluginIds) {
			dependencyPlugin(pluginId);
		}
		return this;
	}

	public JadxPluginInfoBuilder dependencyPlugins(Collection<String> pluginIds) {
		Objects.requireNonNull(pluginIds);
		for (String pluginId : pluginIds) {
			dependencyPlugin(pluginId);
		}
		return this;
	}

	public JadxPluginInfo build() {
		Objects.requireNonNull(pluginId, "PluginId is required");
		Objects.requireNonNull(name, "Name is required");
		Objects.requireNonNull(description, "Description is required");
		if (provides == null) {
			provides = pluginId;
		}
		if (requiredJadxVersion != null) {
			VerifyRequiredVersion.verify(requiredJadxVersion);
		}
		if (parentPlugin != null) {
			if (parentPlugin.isBlank()) {
				throw new IllegalArgumentException("Empty parentPlugin for plugin: " + pluginId);
			}
			if (parentPlugin.equals(pluginId)) {
				throw new IllegalArgumentException("Plugin cannot be its own parent: " + pluginId);
			}
		}
		for (String dep : dependencyPlugins) {
			if (dep.isBlank()) {
				throw new IllegalArgumentException("Empty dependency plugin for plugin: " + pluginId);
			}
			if (dep.equals(pluginId)) {
				throw new IllegalArgumentException("Plugin cannot depend on itself: " + pluginId);
			}
		}
		JadxPluginInfo pluginInfo = new JadxPluginInfo(pluginId, name, description, homepage, provides);
		pluginInfo.setRequiredJadxVersion(requiredJadxVersion);
		pluginInfo.setParentPlugin(parentPlugin);
		pluginInfo.setDependencyPlugins(dependencyPlugins);
		pluginInfo.setDependencyLocations(dependencyLocations);
		return pluginInfo;
	}
}
