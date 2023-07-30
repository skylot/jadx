plugins {
	id("jadx-library")
	id("jadx-kotlin")
}

dependencies {
	implementation(project(":jadx-plugins:jadx-script:jadx-script-runtime"))

	implementation(kotlin("scripting-common"))
	implementation(kotlin("scripting-jvm"))
	implementation(kotlin("scripting-jvm-host"))

	implementation("io.github.oshai:kotlin-logging-jvm:5.0.1")

	testImplementation(project(":jadx-core"))
}
