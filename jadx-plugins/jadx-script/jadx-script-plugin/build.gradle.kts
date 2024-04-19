plugins {
	id("jadx-library")
	id("jadx-kotlin")
}

dependencies {
	implementation(project(":jadx-plugins:jadx-script:jadx-script-runtime"))

	implementation(kotlin("scripting-common"))
	implementation(kotlin("scripting-jvm"))
	implementation(kotlin("scripting-jvm-host"))

	implementation("io.github.oshai:kotlin-logging-jvm:6.0.9")

	// path for scripts cache
	implementation("dev.dirs:directories:26")

	testImplementation(project(":jadx-core"))
}
