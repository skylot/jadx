package jadx.plugins.tools.data;

public class JadxPluginListEntry {
	private String pluginId;
	private String locationId;
	private String name;
	private String description;
	private String homepage;
	private int revision;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getHomepage() {
		return homepage;
	}

	public void setHomepage(String homepage) {
		this.homepage = homepage;
	}

	public String getLocationId() {
		return locationId;
	}

	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPluginId() {
		return pluginId;
	}

	public void setPluginId(String pluginId) {
		this.pluginId = pluginId;
	}

	public int getRevision() {
		return revision;
	}

	public void setRevision(int revision) {
		this.revision = revision;
	}

	@Override
	public String toString() {
		return "JadxPluginListEntry{"
				+ "description='" + description + '\''
				+ ", pluginId='" + pluginId + '\''
				+ ", locationId='" + locationId + '\''
				+ ", name='" + name + '\''
				+ ", homepage='" + homepage + '\''
				+ ", revision=" + revision
				+ '}';
	}
}
