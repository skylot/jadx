package jadx.plugins.input.xapk.data;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class XApkManifest {
	@SerializedName("xapk_version")
	int version;
	@SerializedName("split_apks")
	List<SplitApk> splitApks;

	public List<SplitApk> getSplitApks() {
		return splitApks;
	}

	public void setSplitApks(List<SplitApk> splitApks) {
		this.splitApks = splitApks;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}
}
