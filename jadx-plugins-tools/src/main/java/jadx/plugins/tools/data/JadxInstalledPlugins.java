package jadx.plugins.tools.data;

import java.util.ArrayList;
import java.util.List;

public class JadxInstalledPlugins {

	private long updated;

	private List<JadxPluginMetadata> installed = new ArrayList<>();

	public long getUpdated() {
		return updated;
	}

	public void setUpdated(long updated) {
		this.updated = updated;
	}

	public List<JadxPluginMetadata> getInstalled() {
		return installed;
	}

	public void setInstalled(List<JadxPluginMetadata> installed) {
		this.installed = installed;
	}
}
