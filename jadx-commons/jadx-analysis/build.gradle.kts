plugins {
	id("jadx-library")
}

dependencies {
	implementation(project(":jadx-core"))

	implementation("com.google.code.gson:gson:2.13.2")

	testRuntimeOnly(project(":jadx-plugins:jadx-dex-input"))
	testRuntimeOnly(project(":jadx-plugins:jadx-smali-input"))
}
