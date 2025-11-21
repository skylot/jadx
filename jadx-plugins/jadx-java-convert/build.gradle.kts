plugins {
	id("jadx-library")
}

dependencies {
	api(project(":jadx-core"))

	implementation(project(":jadx-plugins:jadx-dex-input"))
	implementation("com.jakewharton.android.repackaged:dalvik-dx:16.0.1")
	implementation("com.android.tools:r8:8.13.17")

	implementation("org.ow2.asm:asm:9.9")
}
