plugins {
	id("jadx-library")
	id("jadx-kotlin")
}

dependencies {
	api(project(":jadx-core"))

	implementation(project(":jadx-plugins:jadx-dex-input"))
	implementation("com.google.code.gson:gson:2.12.1")
}
