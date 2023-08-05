package jadx.api.plugins;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

public class JadxPluginInfoBuilder {
	private String pluginId;
	private String name;
	private String description;
	private String homepage = "";
	private @Nullable String provides;

	public JadxPluginInfoBuilder() {
	}

	public JadxPluginInfoBuilder pluginId(String pluginId) {
		this.pluginId = Objects.requireNonNull(pluginId);
		return this;
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

	public JadxPluginInfo build() {
		Objects.requireNonNull(pluginId, "PluginId is required");
		Objects.requireNonNull(name, "Name is required");
		Objects.requireNonNull(description, "Description is required");
		if (provides == null) {
			provides = pluginId;
		}
		return new JadxPluginInfo(pluginId, name, description, homepage, provides);
	}
}
