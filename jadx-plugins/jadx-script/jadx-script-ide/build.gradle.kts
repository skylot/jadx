plugins {
	id("jadx-kotlin")
}

dependencies {
	implementation(project(":jadx-plugins:jadx-script:jadx-script-runtime"))
	implementation(project(":jadx-plugins:jadx-script:jadx-script-plugin"))

	implementation(kotlin("scripting-common"))
	implementation(kotlin("scripting-jvm"))
	implementation(kotlin("scripting-compiler-embeddable"))
	implementation(kotlin("scripting-ide-services"))

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
	implementation("io.github.oshai:kotlin-logging-jvm:6.0.4")
}
