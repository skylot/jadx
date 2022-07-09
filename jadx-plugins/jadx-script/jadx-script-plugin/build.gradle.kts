plugins {
	id("jadx-library")

    kotlin("jvm") version "1.7.20"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-scripting-common")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host")

	implementation(project(":jadx-plugins:jadx-script:jadx-script-runtime"))

	implementation("io.github.microutils:kotlin-logging-jvm:3.0.2")
}
