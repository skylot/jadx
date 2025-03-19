plugins {
	id("jadx-library")
}

dependencies {
	api(project(":jadx-core"))

	implementation(project(":jadx-commons:jadx-app-commons"))

	implementation("com.google.code.gson:gson:2.12.1")
}
