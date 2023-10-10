package jadx.plugins.tools.data;

import java.util.List;

public class JadxPluginListCache {
	private String version;
	private List<JadxPluginMetadata> list;

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public List<JadxPluginMetadata> getList() {
		return list;
	}

	public void setList(List<JadxPluginMetadata> list) {
		this.list = list;
	}
}
