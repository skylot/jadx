plugins {
	id("jadx-library")
}

dependencies {
	api(project(":jadx-core"))

	implementation(kotlin("stdlib"))
	implementation(kotlin("scripting-common"))
	implementation(kotlin("scripting-jvm"))

	// allow to use maven dependencies in scripts
	implementation(kotlin("scripting-dependencies"))
	implementation(kotlin("scripting-dependencies-maven"))

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
	implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

	runtimeOnly(project(":jadx-plugins:jadx-dex-input"))
	runtimeOnly(project(":jadx-plugins:jadx-smali-input"))
	runtimeOnly(project(":jadx-plugins:jadx-java-convert"))
	runtimeOnly(project(":jadx-plugins:jadx-java-input"))
	runtimeOnly(project(":jadx-plugins:jadx-raung-input"))

}
