package jadx.plugins.tools.data;

import java.util.ArrayList;
import java.util.List;

public class JadxInstalledPlugins {
	private int version;
	private long updated;
	private List<JadxPluginMetadata> installed = new ArrayList<>();
	private List<String> toDelete = new ArrayList<>();

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

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

	public List<String> getToDelete() {
		return toDelete;
	}

	public void setToDelete(List<String> toDelete) {
		this.toDelete = toDelete;
	}
}
