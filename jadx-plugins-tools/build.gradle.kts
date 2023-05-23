plugins {
	id("jadx-library")
}

dependencies {
	api(project(":jadx-core"))

	implementation("dev.dirs:directories:26")
	implementation("com.google.code.gson:gson:2.10.1")
}
