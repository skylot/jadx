plugins {
    kotlin("jvm") version "1.7.20"
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-scripting-common")
	implementation("org.jetbrains.kotlin:kotlin-scripting-jvm")
	implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable")
	implementation("org.jetbrains.kotlin:kotlin-scripting-ide-services")

	implementation(project(":jadx-plugins:jadx-script:jadx-script-runtime"))
	implementation(project(":jadx-plugins:jadx-script:jadx-script-plugin"))

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
	implementation("io.github.microutils:kotlin-logging-jvm:3.0.2")
}
