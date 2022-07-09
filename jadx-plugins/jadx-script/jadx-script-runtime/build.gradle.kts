plugins {
	id("jadx-library")

    kotlin("jvm") version "1.7.20"
}

group = "jadx-script-context"

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-scripting-common")
	implementation("org.jetbrains.kotlin:kotlin-scripting-jvm")

	// allow to use maven dependencies in scripts
	implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies")
	implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
	implementation("io.github.microutils:kotlin-logging-jvm:3.0.2")

	api(project(":jadx-plugins:jadx-plugins-api"))
	api(project(":jadx-core")) // TODO: workaround

	runtimeOnly(project(":jadx-plugins:jadx-dex-input"))
	runtimeOnly(project(":jadx-plugins:jadx-smali-input"))
	runtimeOnly(project(":jadx-plugins:jadx-java-convert"))
	runtimeOnly(project(":jadx-plugins:jadx-java-input"))
	runtimeOnly(project(":jadx-plugins:jadx-raung-input"))

}
