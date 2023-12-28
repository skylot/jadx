package jadx.plugins.tools.data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

	public void addInstalled(List<JadxPluginMetadata> installed) {
		Set<JadxPluginMetadata> installedSet = new LinkedHashSet<>(this.installed);
		installedSet.addAll(installed);
		this.installed = new ArrayList<>(installedSet);
	}
}
