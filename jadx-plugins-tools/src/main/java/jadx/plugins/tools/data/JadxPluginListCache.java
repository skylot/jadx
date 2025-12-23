package jadx.plugins.tools.data;

import java.util.List;

public class JadxPluginListCache {
	private String version;
	private List<JadxPluginListEntry> list;

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public List<JadxPluginListEntry> getList() {
		return list;
	}

	public void setList(List<JadxPluginListEntry> list) {
		this.list = list;
	}
}
