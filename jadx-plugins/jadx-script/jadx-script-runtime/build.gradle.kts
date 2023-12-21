plugins {
	id("jadx-library")
	id("jadx-kotlin")
}

dependencies {
	api(project(":jadx-core"))

	implementation(kotlin("stdlib"))
	implementation(kotlin("scripting-common"))
	implementation(kotlin("scripting-jvm"))

	// allow to use maven dependencies in scripts
	implementation(kotlin("scripting-dependencies"))
	implementation(kotlin("scripting-dependencies-maven"))

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
	implementation("io.github.oshai:kotlin-logging-jvm:6.0.1")

	runtimeOnly(project(":jadx-plugins:jadx-dex-input"))
	runtimeOnly(project(":jadx-plugins:jadx-smali-input"))
	runtimeOnly(project(":jadx-plugins:jadx-java-convert"))
	runtimeOnly(project(":jadx-plugins:jadx-java-input"))
	runtimeOnly(project(":jadx-plugins:jadx-raung-input"))
}
