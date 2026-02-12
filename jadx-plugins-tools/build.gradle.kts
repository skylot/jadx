plugins {
	id("jadx-java")
	id("jadx-library")
}

dependencies {
	api(project(":jadx-core"))

	implementation(project(":jadx-commons:jadx-app-commons"))

	implementation("com.google.code.gson:gson:2.13.2")
	implementation("commons-io:commons-io:2.21.0")

	testImplementation("com.squareup.okhttp3:mockwebserver3:5.3.0")
}
