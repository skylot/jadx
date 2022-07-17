plugins {
    kotlin("jvm") version "1.7.10"
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-scripting-common")
	implementation("org.jetbrains.kotlin:kotlin-scripting-jvm")
	implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable")
	implementation("org.jetbrains.kotlin:kotlin-scripting-ide-services")

	implementation(project(":jadx-plugins:jadx-script:jadx-script-runtime"))
	implementation(project(":jadx-plugins:jadx-script:jadx-script-plugin"))

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.3")
	implementation("io.github.microutils:kotlin-logging-jvm:2.1.20")
}
