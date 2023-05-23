package jadx.plugins.tools.data;

public class JadxPluginUpdate {
	private final JadxPluginMetadata oldVersion;
	private final JadxPluginMetadata newVersion;

	public JadxPluginUpdate(JadxPluginMetadata oldVersion, JadxPluginMetadata newVersion) {
		this.oldVersion = oldVersion;
		this.newVersion = newVersion;
	}

	public JadxPluginMetadata getOld() {
		return oldVersion;
	}

	public JadxPluginMetadata getNew() {
		return newVersion;
	}

	public String getPluginId() {
		return newVersion.getPluginId();
	}

	public String getOldVersion() {
		return oldVersion.getVersion();
	}

	public String getNewVersion() {
		return newVersion.getVersion();
	}

	@Override
	public String toString() {
		return "PluginUpdate{" + oldVersion.getPluginId()
				+ ": " + oldVersion.getVersion() + " -> " + newVersion.getVersion() + "}";
	}
}
