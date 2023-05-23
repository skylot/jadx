package jadx.plugins.tools.data;

import org.jetbrains.annotations.NotNull;

public class JadxPluginMetadata implements Comparable<JadxPluginMetadata> {
	private String pluginId;
	private String name;
	private String description;
	private String version;
	private String locationId;
	private String resolverId;
	private String jar;

	public String getPluginId() {
		return pluginId;
	}

	public void setPluginId(String pluginId) {
		this.pluginId = pluginId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLocationId() {
		return locationId;
	}

	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	public String getResolverId() {
		return resolverId;
	}

	public void setResolverId(String resolverId) {
		this.resolverId = resolverId;
	}

	public String getJar() {
		return jar;
	}

	public void setJar(String jar) {
		this.jar = jar;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof JadxPluginMetadata)) {
			return false;
		}
		return pluginId.equals(((JadxPluginMetadata) other).pluginId);
	}

	@Override
	public int hashCode() {
		return pluginId.hashCode();
	}

	@Override
	public int compareTo(@NotNull JadxPluginMetadata o) {
		return pluginId.compareTo(o.pluginId);
	}

	@Override
	public String toString() {
		return "JadxPluginMetadata{"
				+ "id=" + pluginId
				+ ", name=" + name
				+ ", version=" + version
				+ ", locationId=" + locationId
				+ ", jar=" + jar
				+ '}';
	}
}
