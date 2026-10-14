plugins {
	id("jadx-library")
}

description = "Native MCP (Model Context Protocol) server for jadx"

dependencies {
	api(project(":jadx-core"))

	implementation(libs.gson)

	testImplementation(project(":jadx-plugins:jadx-java-input"))
}
