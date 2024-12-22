package jadx.plugins.input.apkm

import com.google.gson.annotations.SerializedName

data class ApkmManifest(
	@SerializedName("apkm_version")
	var apkmVersion: Int = -1,
)
