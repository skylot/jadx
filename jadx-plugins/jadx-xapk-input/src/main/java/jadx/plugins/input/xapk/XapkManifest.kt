package jadx.plugins.input.xapk

import com.google.gson.annotations.SerializedName

data class XapkManifest(
	@SerializedName("xapk_version")
	val xapkVersion: Int,
	@SerializedName("split_apks")
	val splitApks: List<SplitApk>,
) {
	data class SplitApk(
		@SerializedName("file")
		val file: String,
		@SerializedName("id")
		val id: String,
	)
}
