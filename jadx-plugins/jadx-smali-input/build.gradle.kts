plugins {
	id("jadx-library")
}

dependencies {
	api(project(":jadx-core"))

	implementation(project(":jadx-plugins:jadx-dex-input"))

	implementation("com.android.tools.smali:smali:3.0.7") {
		exclude(group = "com.beust", module = "jcommander") // exclude old jcommander namespace
	}
	implementation("com.google.guava:guava:33.2.1-jre") // force the latest version for smali
}
