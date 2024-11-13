package jadx.plugins.input.xapk

import com.google.gson.annotations.SerializedName

data class XapkManifest(
	@SerializedName("xapk_version")
	var xapkVersion: Int = 0,
	@SerializedName("split_apks")
	var splitApks: List<SplitApk> = listOf(),
) {
	data class SplitApk(
		@SerializedName("file")
		var file: String = "",
		@SerializedName("id")
		var id: String = "",
	)
}
